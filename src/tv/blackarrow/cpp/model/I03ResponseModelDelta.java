package tv.blackarrow.cpp.model;

import java.util.ArrayList;
import java.util.List;

/*
 * This class hold the any meta data needed to convert between I03->I01 later back and forth.
 * Currently, its used to carry altcontentIdentity/Zone information. But can be used to carry any 
 * further meta-datas too.
 * You may have seen it used as Map<String, I03ResponseModelDelta>: i.e. for each response Signal This VO could be used to carry different meta-data.
 * Here List<I03ResponseModelDelta>, represents for different Zone could have different URL or may have multiple zones in QAM inband case.
 */
public class I03ResponseModelDelta {

	List<AlternateContentTypeModel> alternateContentIDList;

	public List<AlternateContentTypeModel> getAlternateContentIDList() {
		if (alternateContentIDList == null) {
			alternateContentIDList = new ArrayList<>();
		}
		return alternateContentIDList;
	}

	public void setAlternateContentIDList(List<AlternateContentTypeModel> alternateContentIDList) {
		this.alternateContentIDList = alternateContentIDList;
	}

}
