
package org.example.generated;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "healthInsurance",
    "retirement401k"
})
@Generated("jsonschema2pojo")
public class Benefits {

    @JsonProperty("healthInsurance")
    private Boolean healthInsurance;
    @JsonProperty("retirement401k")
    private Boolean retirement401k;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    @JsonProperty("healthInsurance")
    public Boolean getHealthInsurance() {
        return healthInsurance;
    }

    @JsonProperty("healthInsurance")
    public void setHealthInsurance(Boolean healthInsurance) {
        this.healthInsurance = healthInsurance;
    }

    public Benefits withHealthInsurance(Boolean healthInsurance) {
        this.healthInsurance = healthInsurance;
        return this;
    }

    @JsonProperty("retirement401k")
    public Boolean getRetirement401k() {
        return retirement401k;
    }

    @JsonProperty("retirement401k")
    public void setRetirement401k(Boolean retirement401k) {
        this.retirement401k = retirement401k;
    }

    public Benefits withRetirement401k(Boolean retirement401k) {
        this.retirement401k = retirement401k;
        return this;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    public Benefits withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Benefits.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("healthInsurance");
        sb.append('=');
        sb.append(((this.healthInsurance == null)?"<null>":this.healthInsurance));
        sb.append(',');
        sb.append("retirement401k");
        sb.append('=');
        sb.append(((this.retirement401k == null)?"<null>":this.retirement401k));
        sb.append(',');
        sb.append("additionalProperties");
        sb.append('=');
        sb.append(((this.additionalProperties == null)?"<null>":this.additionalProperties));
        sb.append(',');
        if (sb.charAt((sb.length()- 1)) == ',') {
            sb.setCharAt((sb.length()- 1), ']');
        } else {
            sb.append(']');
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result* 31)+((this.healthInsurance == null)? 0 :this.healthInsurance.hashCode()));
        result = ((result* 31)+((this.retirement401k == null)? 0 :this.retirement401k.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Benefits) == false) {
            return false;
        }
        Benefits rhs = ((Benefits) other);
        return ((((this.healthInsurance == rhs.healthInsurance)||((this.healthInsurance!= null)&&this.healthInsurance.equals(rhs.healthInsurance)))&&((this.retirement401k == rhs.retirement401k)||((this.retirement401k!= null)&&this.retirement401k.equals(rhs.retirement401k))))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))));
    }

}
