/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.tablespoon.client.api;

import se.kth.tablespoon.client.events.EventDefinition;

/**
 *
 * @author henke
 */
public interface TablespoonAPI {
  
  
  public void subscribeToEvent(EventDefinition ed);
  
  
}
