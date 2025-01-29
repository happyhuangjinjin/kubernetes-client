
package io.fabric8.kubernetes.api.model.clusterapi.v1beta1;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.builder.Editable;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.sundr.builder.annotations.Buildable;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

@JsonDeserialize(using = com.fasterxml.jackson.databind.JsonDeserializer.None.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "nodeDrainStartTime",
    "waitForNodeVolumeDetachStartTime"
})
@ToString
@EqualsAndHashCode
@Accessors(prefix = {
    "_",
    ""
})
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false, lazyCollectionInitEnabled = false, builderPackage = "io.fabric8.kubernetes.api.builder")
@Generated("io.fabric8.kubernetes.schema.generator.model.ModelGenerator")
public class MachineDeletionStatus implements Editable<MachineDeletionStatusBuilder>, KubernetesResource
{

    @JsonProperty("nodeDrainStartTime")
    private String nodeDrainStartTime;
    @JsonProperty("waitForNodeVolumeDetachStartTime")
    private String waitForNodeVolumeDetachStartTime;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    /**
     * No args constructor for use in serialization
     */
    public MachineDeletionStatus() {
    }

    public MachineDeletionStatus(String nodeDrainStartTime, String waitForNodeVolumeDetachStartTime) {
        super();
        this.nodeDrainStartTime = nodeDrainStartTime;
        this.waitForNodeVolumeDetachStartTime = waitForNodeVolumeDetachStartTime;
    }

    @JsonProperty("nodeDrainStartTime")
    public String getNodeDrainStartTime() {
        return nodeDrainStartTime;
    }

    @JsonProperty("nodeDrainStartTime")
    public void setNodeDrainStartTime(String nodeDrainStartTime) {
        this.nodeDrainStartTime = nodeDrainStartTime;
    }

    @JsonProperty("waitForNodeVolumeDetachStartTime")
    public String getWaitForNodeVolumeDetachStartTime() {
        return waitForNodeVolumeDetachStartTime;
    }

    @JsonProperty("waitForNodeVolumeDetachStartTime")
    public void setWaitForNodeVolumeDetachStartTime(String waitForNodeVolumeDetachStartTime) {
        this.waitForNodeVolumeDetachStartTime = waitForNodeVolumeDetachStartTime;
    }

    @JsonIgnore
    public MachineDeletionStatusBuilder edit() {
        return new MachineDeletionStatusBuilder(this);
    }

    @JsonIgnore
    public MachineDeletionStatusBuilder toBuilder() {
        return edit();
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    public void setAdditionalProperties(Map<String, Object> additionalProperties) {
        this.additionalProperties = additionalProperties;
    }

}
