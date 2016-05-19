/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/
package se.kth.tablespoon.client.broadcasting;

import com.aphyr.riemann.Proto.Event;
import com.aphyr.riemann.client.RiemannBatchClient;
import com.aphyr.riemann.client.RiemannClient;
import java.io.IOException;
import java.util.List;
import se.kth.tablespoon.client.api.Subscriber;
import se.kth.tablespoon.client.topics.Topic;
import se.kth.tablespoon.client.util.Sleep;

/**
 *
 * @author henke
 */
public class EventFetcher {
  
  private final Subscriber subscriber;
  private final Topic topic;
  
  public EventFetcher(Subscriber subscriber, Topic topic) {
    this.subscriber = subscriber;
    this.topic = topic;
  }
  
  
  public void queryRiemann(RiemannClient rClient) throws IOException {
//    List<Event> events =  rClient.query("service = \"" + topic.getUniqueId() + "\"").deref();
     List<Event> events =  rClient.query("service = \"a0986132-a503-4cee-ae85-83ac17ddcec1\"").deref();
      System.out.println(events.size());
    for (Event event : events) {
      System.out.println(event.getTime());
      Sleep.now(100);
    }
  }
  
  
  
}