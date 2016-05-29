/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/
package se.kth.tablespoon.client.broadcasting;

import com.aphyr.riemann.Proto.Event;
import se.kth.tablespoon.client.api.TablespoonEvent;
import se.kth.tablespoon.client.events.CollectlMapping;
import se.kth.tablespoon.client.events.ResourceType;
import se.kth.tablespoon.client.topics.Topic;

/**
 *
 * @author henke
 */
class EventConverter {
  
  static TablespoonEvent changeFormat(Event event, Topic topic) {
    ResourceType resourceType = CollectlMapping.getInstance().getResourceType(topic.getIndex());
    TablespoonEvent tablespoonEvent = new TablespoonEvent(topic.getGroupId(),
        event.getHost(),
        event.getMetricD(),
        topic.getEventType(),
        resourceType,
        topic.getHigh(),
        topic.getLow());
    return tablespoonEvent;
  }
  
}