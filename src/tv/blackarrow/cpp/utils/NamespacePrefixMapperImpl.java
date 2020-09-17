package tv.blackarrow.cpp.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.xml.bind.marshaller.NamespacePrefixMapper;

/**
 * This was migrated from tv.blackarrow.integration.dvs.util.NamespacePrefixMapperImpl
 * 
 * @author yhuang
 * @version 2.0
 * @date Aug 24, 2011
 */
public class NamespacePrefixMapperImpl extends NamespacePrefixMapper {
	protected final static Logger log = LogManager.getLogger(NamespacePrefixMapperImpl.class);
	
	public NamespacePrefixMapperImpl() {
		super();
	}

	/**
	 * Returns a preferred prefix for the given namespace URI. This method is
	 * intended to be override by a derived class.
	 * 
	 * @param namespaceUri The namespace URI for which the prefix needs to be
	 *            found. Never be null. "" is used to denote the default
	 *            namespace.
	 * @param suggestion When the content tree has a suggestion for the prefix
	 *            to the given namespaceUri, that suggestion is passed as a
	 *            parameter. Typical this value comes from the QName.getPrefix
	 *            to show the preference of the content tree. This parameter may
	 *            be null, and this parameter may represent an already occupied
	 *            prefix.
	 * @param requirePrefix If this method is expected to return non-empty
	 *            prefix. When this flag is true, it means that the given
	 *            namespace URI cannot be set as the default namespace.
	 * @return null if there's no preferred prefix for the namespace URI. In
	 *         this case, the system will generate a prefix for you. Otherwise
	 *         the system will try to use the returned prefix, but generally
	 *         there's no guarantee if the prefix will be actually used or not.
	 *         return "" to map this namespace URI to the default namespace.
	 *         Again, there's no guarantee that this preference will be honored.
	 *         If this method returns "" when requirePrefix=true, the return
	 *         value will be ignored and the system will generate one.
	 */
	public String getPreferredPrefix(String namespaceUri, String suggestion, boolean requirePrefix) {
		// I want this namespace to be mapped to "xsi"
		if ("http://scte.org/dvs629/data".equals(namespaceUri))
			return "dat";

		// I want the namespace foo to be the default namespace.
		if ("http://scte.org/dvs629/vod".equals(namespaceUri))
			return "vod";

		// and the namespace bar will use "b".
		if ("http://scte.org/dvs629/msg".equals(namespaceUri))
			return "msg";

		if ("http://scte.org/dvs629/2007/core".equalsIgnoreCase(namespaceUri))
			return "core";

		if ("http://scte.org/dvs629/2007/cis".equalsIgnoreCase(namespaceUri))
			return "cis";

		// namespace for the ballot Dec 2007 DVS629
		if ("http://www.scte.org/schemas/629-2/2007/core".equalsIgnoreCase(namespaceUri))
			return "core";

		if ("http://www.scte.org/schemas/629-3/2007/adm".equalsIgnoreCase(namespaceUri))
			return "adm";

		// namespace for the ballot Feb 2008, SCTE130
		if ("http://www.scte.org/schemas/130-3/2008/adm".equalsIgnoreCase(namespaceUri))
			return "adm";

		if ("http://www.scte.org/schemas/130-2/2008/core".equalsIgnoreCase(namespaceUri))
			return "core";

		// namespace for the 2008a, SCTE130
		if ("http://www.scte.org/schemas/130-3/2008a/adm".equalsIgnoreCase(namespaceUri))
			return "adm";
		
		if ("http://www.scte.org/schemas/130-3/2008a/adm/podm".equalsIgnoreCase(namespaceUri))
			return "podm";

		if ("http://www.scte.org/schemas/130-2/2008a/core".equalsIgnoreCase(namespaceUri))
			return "core";

		if ("http://www.scte.org/schemas/130-5/2008/pois".equalsIgnoreCase(namespaceUri))
			return "pois";

		if ("http://www.scte.org/schemas/130-8/2008/gis".equalsIgnoreCase(namespaceUri))
			return "gis";

		if ("http://www.scte.org/schemas/130-8/2009/gis".equalsIgnoreCase(namespaceUri))
			return "gis";

		if ("http://www.scte.org/schemas/130-6/2008/sis".equalsIgnoreCase(namespaceUri))
			return "sis";

		if ("http://www.comcast.com/schemas/NGOD/P1/2008/R0V0".equalsIgnoreCase(namespaceUri))
			return "cmcst";

		if ("http://www.comcast.com/schemas/NGOD/P1/2008/R1V0".equalsIgnoreCase(namespaceUri))
			return "cmcst";

		if ("http://www.scte.org/schemas/130-4/2008a/cis".equalsIgnoreCase(namespaceUri))
			return "cis";

		if ("http://www.scte.org/schemas/130-6/2008a/sis".equalsIgnoreCase(namespaceUri))
			return "sis";

		if ("http://www.scte.org/schemas/130-4/2008/cis".equalsIgnoreCase(namespaceUri))
			return "cis";

		if ("http://www.scte.org/schemas/130-5/2009/pois".equalsIgnoreCase(namespaceUri))
			return "pois";

		if ("http://www.scte.org/schemas/130-4/2009/cis".equalsIgnoreCase(namespaceUri))
			return "cis";

		if ("http://www.scte.org/schemas/130-6/2009/sis".equalsIgnoreCase(namespaceUri))
			return "sis";

		if ("http://schemas.xmlsoap.org/soap/envelope/".equalsIgnoreCase(namespaceUri))
			return "soapenv";

		if ("http://www.cablelabs.com/namespaces/metadata/xsd/signaling/2".equalsIgnoreCase(namespaceUri))
			return "sig";
		
		if ("urn:cablelabs:md:xsd:signaling:3.0".equalsIgnoreCase(namespaceUri))
			return "sig";

		if ("http://www.cablelabs.com/namespaces/metadata/xsd/conditioning/2".equalsIgnoreCase(namespaceUri))
			return "cond";

		if ("http://www.cablelabs.com/namespaces/metadata/xsd/core/2".equalsIgnoreCase(namespaceUri))
			return "core";
		
		// Per charter's request, make ns2 prefix
		if ("urn:cablelabs:iptvservices:esam:xsd:signal:1".equalsIgnoreCase(namespaceUri))
			return "ns2";
			
		return suggestion;
	}

	/**
	 * Returns a list of namespace URIs that should be declared at the root
	 * element.
	 * <p>
	 * By default, the JAXB RI produces namespace declarations only when they
	 * are necessary, only at where they are used. Because of this lack of
	 * look-ahead, sometimes the marshaller produces a lot of namespace
	 * declarations that look redundant to human eyes. For example,
	 * 
	 * <pre>
	 * &lt;xmp&gt;
	 * &lt;?xml version=&quot;1.0&quot;?&gt;
	 * &lt;root&gt;
	 *   &lt;ns1:child xmlns:ns1=&quot;urn:foo&quot;&gt; ... &lt;/ns1:child&gt;
	 *   &lt;ns2:child xmlns:ns2=&quot;urn:foo&quot;&gt; ... &lt;/ns2:child&gt;
	 *   &lt;ns3:child xmlns:ns3=&quot;urn:foo&quot;&gt; ... &lt;/ns3:child&gt;
	 *   ...
	 * &lt;/root&gt;
	 * &lt;xmp&gt;
	 * </pre>
	 * <p>
	 * If you know in advance that you are going to use a certain set of
	 * namespace URIs, you can override this method and have the marshaller
	 * declare those namespace URIs at the root element.
	 * <p>
	 * For example, by returning <code>new String[]{"urn:foo"}</code>, the
	 * marshaller will produce:
	 * 
	 * <pre>
	 * &lt;xmp&gt;
	 * &lt;?xml version=&quot;1.0&quot;?&gt;
	 * &lt;root xmlns:ns1=&quot;urn:foo&quot;&gt;
	 *   &lt;ns1:child&gt; ... &lt;/ns1:child&gt;
	 *   &lt;ns1:child&gt; ... &lt;/ns1:child&gt;
	 *   &lt;ns1:child&gt; ... &lt;/ns1:child&gt;
	 *   ...
	 * &lt;/root&gt;
	 * &lt;xmp&gt;
	 * </pre>
	 * <p>
	 * To control prefixes assigned to those namespace URIs, use the
	 * {@link #getPreferredPrefix} method.
	 * 
	 * @return A list of namespace URIs as an array of {@link String}s. This
	 *         method can return a length-zero array but not null. None of the
	 *         array component can be null. To represent the empty namespace,
	 *         use the empty string <code>""</code>.
	 * @since JAXB RI 1.0.2
	 */
	// public String[] getPreDeclaredNamespaceUris() {
	// return new String[] { "urn:abc", "urn:def" };
	// }

}
