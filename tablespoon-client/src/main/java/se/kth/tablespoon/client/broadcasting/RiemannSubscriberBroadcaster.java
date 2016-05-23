/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/
package se.kth.tablespoon.client.broadcasting;

import com.aphyr.riemann.client.RiemannClient;
import java.io.IOException;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.tablespoon.client.api.Subscriber;
import se.kth.tablespoon.client.topics.Topic;
import se.kth.tablespoon.client.util.Time;

/**
 *
 * @author henke
 */
public class RiemannSubscriberBroadcaster implements Runnable, SubscriberBroadcaster {
  
  private final static Logger slf4jLogger = LoggerFactory.getLogger(SubscriberBroadcaster.class);
  ArrayList<RiemannEventFetcher> fetchers = new ArrayList<>();
  RiemannClient riemannClient;
  private final String host;
  private final int port;
  private final int RECONNECTION_TIME = 5000;
  private final int RECONNECTION_TRIES = 100;
  private int tries = 0;
  
  public RiemannSubscriberBroadcaster(String host, int port) {
    this.host = host;
    this.port = port;
  }
  
  private void broadcastEvents() throws IOException {
    while(true) {
      for (RiemannEventFetcher fetcher : fetchers) {
        if (fetcher.shouldQuery()) fetcher.queryRiemannAndSend(riemannClient);
      }
    }
  }
  
  @Override
  public void registerSubscriber(Subscriber subscriber, Topic topic) {
    fetchers.add(new RiemannEventFetcher(subscriber, topic));
  }
  
  @Override
  public void run() {
    broadcast();
  }
  
  @Override
  public void broadcast() {    
    try {
      connect();
      //resetting number of tries if connection was established
      tries = 0;
      broadcastEvents();
    } catch (IOException e) {
      if (reconnect(tries)) {
        tries++;
        broadcast();
      }
    }
  }
  
  
  private void connect() throws IOException {
    riemannClient = RiemannClient.tcp(host, port);
    riemannClient.connect();
    slf4jLogger.info("Established connection with host:"
        + host + " port:" + port);
  }
  
  private boolean reconnect(int tries) {
    riemannClient.close();
    slf4jLogger.info("Connection with server could not be established.");
    if (tries < RECONNECTION_TRIES) {
      slf4jLogger.info("Waiting for "
          + Math.round(RECONNECTION_TIME / 1000)
          + " seconds and attempting to connect again...");
      Time.sleep(RECONNECTION_TRIES);
      return true;
    }
    return false;
  }
  
  public void closeRiemannClient() {
    if (riemannClient != null) {
      if (riemannClient.isConnected()) {
        riemannClient.close();
        slf4jLogger.info("Closed the connection with the Riemann client.");
      }
    }
  }
  
}
