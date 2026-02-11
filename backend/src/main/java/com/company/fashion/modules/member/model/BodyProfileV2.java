package com.company.fashion.modules.member.model;

public class BodyProfileV2 {

  private int version = 2;
  private BodyMeasurements measurements;
  private BodyDerivedMetrics derived;

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  public BodyMeasurements getMeasurements() {
    return measurements;
  }

  public void setMeasurements(BodyMeasurements measurements) {
    this.measurements = measurements;
  }

  public BodyDerivedMetrics getDerived() {
    return derived;
  }

  public void setDerived(BodyDerivedMetrics derived) {
    this.derived = derived;
  }
}

