package tv.blackarrow.cpp.components.scc.schedulelessaltevent.response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.utils.SegmentType;

public class ResponseFactory {
	private static final Logger LOGGER = LogManager.getLogger(ResponseFactory.class);

	/**
	 * Clients can be returned based on solr version.
	 *
	 * @param solrVersion
	 * @param url
	 * @return
	 */
	public static IBaseResponseProcessor getClient(SegmentType segmentTypeId) {

		IBaseResponseProcessor response = null;
		switch (segmentTypeId) {
		case PROGRAM_START:
			response = new ProgramStartResponseProcessor();
			break;

		case PROGRAM_END:
			response = new ProgramEndResponseProcessor();
			break;

		case CONTENT_IDENTIFICATION:
			response = new ContentIDResponseProcessor();
			break;
		}

		return response;
	}
}
