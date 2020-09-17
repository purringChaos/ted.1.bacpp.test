package tv.blackarrow.cpp.loader.po;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import tv.blackarrow.cpp.model.POInfo;
import tv.blackarrow.cpp.model.PlacementOpportunity;

public class POJsonParser {

    private static final Logger LOGGER = LogManager.getLogger(POJsonParser.class);

    public List<ZonePO> parseJson(String csvFilename) {
        List<ZonePO> poList = new ArrayList<ZonePO>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(csvFilename));
            String line = null;
            Map<String, PlacementOpportunity> previousPoByZone = new HashMap<String, PlacementOpportunity>();
            while ((line = reader.readLine()) != null) {
                LOGGER.debug("A line of JSON from PO data file: " + line);
                ZonePO vo = parseJsonRow(line);
                // link the POs together here so we don't have to do a bunch of updates 
                // as we add them to the data store later
                PlacementOpportunity previousPO = previousPoByZone.get(vo.getZoneExtRef());
                if (previousPO != null) {
                    previousPO.setNextPOKey(vo.getPlacementOpportunity().getPOKey());
                }
                previousPoByZone.put(vo.getZoneExtRef(), vo.getPlacementOpportunity());
                poList.add(vo);
            }
        } catch (Exception ex) {
            LOGGER.error(()->"Error parsing PO file " + csvFilename, ex);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    LOGGER.error(()->"failed to close file stream " + csvFilename);
                }
            }
        }
        return poList;
    }

    private ZonePO parseJsonRow(String line) {
        ZonePO zonepo = new ZonePO();
        try {
            JSONObject jobj = new JSONObject(line);

            PlacementOpportunity po = new PlacementOpportunity();

            po.setUtcWindowStartTime(jobj.getLong("windowStartTime"));
            po.setWindowDurationMilliseconds(jobj.getInt("windowDuration"));
            po.setPlacementsDurationMilliseconds(jobj.getInt("breakDuration"));
            po.setPOKey(jobj.getString("breakId"));
            po.setInSignalId(jobj.getString("inSignalId"));
            po.setOutSignalId(jobj.getString("outSignalId"));
            po.setBreakOrder(jobj.getInt("breakOrder"));

            // metadata population for the default PO
            // in CCMS case, there will be no metadata for any PO
            JSONArray metadata = jobj.getJSONArray("metaData");
            po.setMetadata(getMetadataMap(metadata));

            // variations handling for ADI 3.0 case
            // in CCMS case, this will be null or empty
            JSONArray variations = jobj.getJSONArray("variations");
            if (variations != null && variations.length() > 0) {
                // ADI 3.0 case, CCMS case will be null or empty

                ArrayList<POInfo> variationsPOs = new ArrayList<POInfo>();

                for (int i = 0; i < variations.length(); i++) {
                    JSONObject variation = variations.getJSONObject(i);

                    POInfo poInfo = new POInfo();

                    poInfo.setDuration(variation.getInt("duration"));
                    poInfo.setPoKey(variation.getString("poId"));
                    poInfo.setInSignalId(variation.getString("inSignalId"));

                    metadata = variation.getJSONArray("metaData");
                    poInfo.setMetadata(getMetadataMap(metadata));

                    variationsPOs.add(poInfo);
                }

                po.setVariations(variationsPOs);
            }

            zonepo.setPo(po);

            zonepo.setZoneExtRef(jobj.getString("zoneExtRef"));
        } catch (JSONException e) {
            LOGGER.error(()->"JSONException while loading PO data file", e);
        }
        return zonepo;
    }

    private HashMap<String, String> getMetadataMap(JSONArray metadata) {
        HashMap<String, String> metadataMap = null;
        if (metadata != null) {
            metadataMap = new HashMap<String, String>();
            int len = metadata.length();
            for (int i = 0; i < len; i++) {
                String key = null;
                try {
                    JSONObject jo = metadata.getJSONObject(i);
                    String[] metaArray = JSONObject.getNames(jo);
                    if (metaArray != null && metaArray.length == 1)
                        key = metaArray[0];
                    if (key != null)
                        metadataMap.put(key, jo.getString(key));
                } catch (JSONException e) {
                    LOGGER.error("Problem retrieving meatadata for key: " + key, e);
                }
            }
        }

        return metadataMap;
    }
}
