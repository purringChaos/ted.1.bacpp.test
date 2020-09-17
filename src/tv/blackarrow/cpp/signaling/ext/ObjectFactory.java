package tv.blackarrow.cpp.signaling.ext;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;
 
@XmlRegistry
public class ObjectFactory {
 
    @XmlElementDecl(name="feed")
    public JAXBElement<Feed> createFeed(Feed feed) {
        return new JAXBElement<Feed>(new QName("feed"), Feed.class, feed);
    }
 
    @XmlElementDecl(name="network")
    public JAXBElement<Network> createNetwork(Network network) {
        return new JAXBElement<Network>(new QName("network"), Network.class, network);
    }
    
    @XmlElementDecl(name="ap")
    public JAXBElement<AP> createAP(AP ap) {
        return new JAXBElement<AP>(new QName("ap"), AP.class, ap);
    }
    
    @XmlElementDecl(name="utc")
    public JAXBElement<UTC> createFeed(UTC utc) {
        return new JAXBElement<UTC>(new QName("utc"), UTC.class, utc);
    }
    
    @XmlElementDecl(name="duration")
    public JAXBElement<Duration> createDuration(Duration duration) {
        return new JAXBElement<Duration>(new QName("duration"), Duration.class, duration);
    }
    
    @XmlElementDecl(name="spliceType")
    public JAXBElement<SpliceType> createSpliceType(SpliceType spliceType) {
        return new JAXBElement<SpliceType>(new QName("spliceType"), SpliceType.class, spliceType);
    }
}