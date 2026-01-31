package com.o3.server;

import org.json.JSONObject;

public class ObservationRecord {
    private String targetBodyName;
    private String centerBodyName;
    private String epoch;
    private JSONObject orbitalElements; 
    private JSONObject stateVector;     

    public ObservationRecord(String targetBodyName, String centerBodyName, String epoch, JSONObject orbitalElements, JSONObject stateVector) {
        this.targetBodyName = targetBodyName;
        this.centerBodyName = centerBodyName;
        this.epoch = epoch;
        this.orbitalElements = orbitalElements;
        this.stateVector = stateVector;
    }

    public JSONObject toJSONObject() {
        JSONObject json = new JSONObject();
        json.put("target_body_name", this.targetBodyName);
        json.put("center_body_name", this.centerBodyName);
        json.put("epoch", this.epoch);
        
        if (this.orbitalElements != null) {
            json.put("orbital_elements", this.orbitalElements);
        }
        
        if (this.stateVector != null) {
            json.put("state_vector", this.stateVector);
        }
        
        return json;
    }
}