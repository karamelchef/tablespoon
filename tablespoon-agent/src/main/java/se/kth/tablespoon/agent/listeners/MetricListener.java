/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.tablespoon.agent.listeners;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.tablespoon.agent.general.Configuration;
import se.kth.tablespoon.agent.main.Start;
import se.kth.tablespoon.agent.metrics.Metric;
import se.kth.tablespoon.agent.metrics.MetricLayout;

/**
 *
 * @author te27
 */
public abstract class MetricListener implements Runnable {

  protected final Queue<Metric> metricQueue = new LinkedList<>();
  protected Process process;
  protected BufferedReader br;
  protected MetricLayout[] mls;
  protected boolean headersDefined = false;
  protected boolean interruptRequest = false;
  protected boolean restartRequest = false;
  protected final Configuration config;
  protected final Logger slf4jLogger = LoggerFactory.getLogger(MetricListener.class);

  public MetricListener(Configuration config) {
    this.config = config;
  }

  @Override
  public void run() {
    collectCycle();
    slf4jLogger.info("Ending metricListener-thread.");
  }

  public abstract void collectCycle();

  protected void emptyOld() {
    Metric metric = metricQueue.peek();
    long ttl = config.getRiemannEventTtl();
    long now = System.currentTimeMillis() / 1000L;
    if (now - metric.getTimeStamp() > ttl) {
      synchronized (metricQueue) {
        metricQueue.remove();
      }
      slf4jLogger.info("Metric was too old and discarded.");
      emptyOld();
    }

  }

  public void requestInterrupt() {
    restartRequest = false;
    interruptRequest = true;
  }

  public void requestRestart() {
    restartRequest = true;
    slf4jLogger.info("Attempting to restart collectl.");
  }

  public Queue<Metric> getMetricQueue() {
    return metricQueue;
  }

  public MetricLayout[] getEventLayouts() {
    return mls;
  }

  public boolean queueIsEmpty() {
    return metricQueue.isEmpty();
  }

  public boolean isRestarting() {
    return restartRequest;
  }

}
