package se.kth.tablespoon.client.topics;

import se.kth.tablespoon.client.events.Threshold;
import se.kth.tablespoon.client.events.EventType;
import com.fasterxml.jackson.jr.ob.JSON;
import com.fasterxml.jackson.jr.ob.JSONComposer;
import com.fasterxml.jackson.jr.ob.comp.ObjectComposer;
import io.riemann.riemann.client.RiemannClient;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import se.kth.tablespoon.client.api.Subscriber;
import se.kth.tablespoon.client.broadcasting.EventFetcher;
import se.kth.tablespoon.client.broadcasting.RiemannEventFetcher;
import se.kth.tablespoon.client.general.Groups;
import se.kth.tablespoon.client.util.RuleSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Topic {
  
  protected final HashSet<String> machinesNotified = new HashSet<>();
  public EventFetcher eventFetcher;
  private final int collectIndex;
  protected final String groupId;
  private final long startTime;
  private final EventType eventType;
  private final int sendRate;
  private int duration = 0;
  private Threshold high;
  private Threshold low;
  private final String uniqueId;
  private String replacesTopicId;
  private String json = "";
  private int retrievalDelay;
  private AtomicBoolean queryBusy = new AtomicBoolean(false);
  private final static Logger logger = LoggerFactory.getLogger(Topic.class);
  
  public Topic(int collectIndex, long startTime, String uniqueId, EventType type, int sendRate, String groupId) {
    this.collectIndex = collectIndex;
    this.startTime = startTime;
    this.uniqueId = uniqueId;
    this.eventType = type;
    this.sendRate = sendRate;
    this.groupId = groupId;
  }
  
  public abstract HashSet<String> getMachinesToNotify();
  
  public abstract void updateMachineState(Groups groups);
  
  public abstract boolean hasNoLiveMachines();
  
  public abstract Set<String> getMachines();
  
  public void addToNotifiedMachines(HashSet<String> machines) {
    machinesNotified.addAll(machines);
  }
  
  public void createFetcher(Subscriber subscriber, RiemannClient riemannClient) {
    eventFetcher = new RiemannEventFetcher(subscriber, this, riemannClient);
  }
  
  public void setDuration(int duration) {
    this.duration = duration;
  }
  
  public void setHigh(Threshold high) {
    this.low = null;
    this.high = high;
  }
  
  public void setLow(Threshold low) throws ThresholdException {
    if (high==null ||
        RuleSupport.getNormalizedComparatorType(high.comparator) ==
        RuleSupport.getNormalizedComparatorType(low.comparator) ||
        high.percentage <= low.percentage) {
      throw new ThresholdException();
    }
    this.low = low;
  }

  public void setRetrievalDelay(int retrievalDelay) {
    this.retrievalDelay = retrievalDelay;
  }
  
  public int getCollectIndex() {
    return collectIndex;
  }
  
  public long getStartTime() {
    return startTime;
  }
  
  public int getDuration() {
    return duration;
  }
  
  public Threshold getHigh() {
    return high;
  }
  
  public Threshold getLow() {
    return low;
  }
  
  public EventType getEventType() {
    return eventType;
  }
  
  public int getSendRate() {
    return sendRate;
  }
  
  public String getUniqueId() {
    return uniqueId;
  }
  
  public String getGroupId() {
    return groupId;
  }
  
  public int getRetrievalDelay() {
    return retrievalDelay;
  }
  
  public void generateJson() throws IOException {
    JSONComposer<String> composer = JSON.std
        .with(JSON.Feature.PRETTY_PRINT_OUTPUT)
        .composeString();
    ObjectComposer obj = composer.startObject();
    obj.put("collectIndex", collectIndex)
        .put("startTime", startTime)
        .put("uniqueId", uniqueId)
        .put("groupId", groupId)
        .put("eventType", eventType.toString())
        .put("sendRate", sendRate);
    if (replacesTopicId != null) obj.put("replacesTopicId", replacesTopicId);
    if (duration > 0) obj.put("duration", duration);
    if (high != null) obj.startObjectField("high")
        .put("percentage", high.percentage)
        .put("comparator", high.comparator.toString())
        .end();
    if (low != null) obj.startObjectField("low")
        .put("percentage", low.percentage)
        .put("comparator", low.comparator.toString())
        .end();
    obj.end();
    json = composer.finish();
  }
  
  public String getJson() {
    return json;
  }
  
  public String getRemovalJson() throws IOException {
    return JSON.std
        .with(JSON.Feature.PRETTY_PRINT_OUTPUT)
        .composeString()
        .startObject()
        .put("collectIndex", collectIndex)
        .put("uniqueId", uniqueId)
        .put("remove", true)
        .end()
        .finish();
  }
  
  public synchronized void queryDone() {
    queryBusy.set(false);
    logger.debug("fetcher finished the query for : " + toString());
  }
  
  
  public void setReplaces(String replacesTopicId, TopicStorage storage) throws MissingTopicException {
    if (storage.uniqueIdExists(replacesTopicId) == false) throw new MissingTopicException();
    this.replacesTopicId = replacesTopicId;
  }
  
  public synchronized void fetch(ExecutorService tpe) {
    logger.debug("check if we can start the fetcher for : " + toString());
    if (queryBusy.get() == false && eventFetcher.shouldQuery(retrievalDelay)) {
      queryBusy.set(true);
      logger.debug("staring fetcher for topic: " + toString());
      tpe.submit(eventFetcher);
    } else {
      logger.debug("Not a good time to start the fetcher for : " + toString());
    }
  }

  @Override
  public String toString(){
    StringBuilder sb = new StringBuilder();
    if(collectIndex <= 20){
      sb.append("CPU ");
    }
    else if(collectIndex <= 49 || collectIndex == 68){
      sb.append("MEM ");
    }
    else if(collectIndex <= 58){
      sb.append("NET ");
    }
    else if(collectIndex <= 67){
      sb.append("DSK ");
    }
    if(high != null){
      sb.append(high.comparator.symbol).append(" ").append(high.percentage).append("%");
      if(low != null){
        sb.append(low.comparator.symbol).append(" ").append(low.comparator).append("%");
      }
    }
    else{
      sb.append("no thresholds");
    }
    sb.append(" for group ").append(groupId);
    return sb.toString();
  }
}
