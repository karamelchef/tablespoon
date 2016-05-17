/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/
package se.kth.tablespoon.agent.general;

import se.kth.tablespoon.agent.events.Configuration;
import com.aphyr.riemann.client.RiemannClient;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.tablespoon.agent.events.Topics;
import se.kth.tablespoon.agent.file.TopicLoader;
import se.kth.tablespoon.agent.listeners.MetricListener;
import se.kth.tablespoon.agent.main.Start;
import se.kth.tablespoon.agent.util.Sleep;

public class Agent {
  
  private static final int READ_QUEUE_TIME = 10;
  
  private final MetricListener metricListener;
  private TopicLoader topicLoader;
  private final Configuration config = Configuration.getInstance();
  private RiemannClient riemannClient;
  private final EventSender es;
  
  public RiemannClient getRiemannClient() {
    return riemannClient;
  }
  private final Logger slf4jLogger = LoggerFactory.getLogger(Start.class);
  
  public Agent(MetricListener metricListener, Topics topics) {
    this.metricListener = metricListener;
    this.topicLoader = new TopicLoader(topics);
    this.es = new EventSender(metricListener.getMetricQueue(), riemannClient, topics);
  }
  
  public void start() {
    agentCycle(0);
  }
  
  private void connect() throws IOException {
    riemannClient = RiemannClient.tcp(config.getRiemannHost(),
        config.getRiemannPort());
    riemannClient.connect();
    slf4jLogger.info("Established connection with host:"
        + config.getRiemannHost() + " port:" + config.getRiemannPort());
  }
  
  private boolean reconnect(int tries) {
    riemannClient.close();
    slf4jLogger.info("Connection with server could not be established.");
    if (tries < config.getRiemannReconnectionTries()) {
      slf4jLogger.info("Waiting for "
          + Math.round(config.getRiemannReconnectionTime() / 1000)
          + " seconds and attempting to connect again...");
      Sleep.now(config.getRiemannReconnectionTime());
      return true;
    }
    return false;
  }
  
  private void sendCycle() throws IOException {
    while (true) {
      topicLoader.readTopicFiles();
      // it apparently needs some headroom.
      Sleep.now(READ_QUEUE_TIME);
      synchronized (metricListener.getMetricQueue()) {
        if (!metricListener.queueIsEmpty()) {
          es.sendMetrics();
        }
      }
    }
  }
  
  
  private void agentCycle(int tries) {
    try {
      System.out.println("Hey trying to reconnect man.");
      connect();
      //resetting number of tries if connection was established
      tries = 0;
      sendCycle();
    } catch (IOException e) {
      if (reconnect(tries)) {
        agentCycle(++tries);
      }
    }
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