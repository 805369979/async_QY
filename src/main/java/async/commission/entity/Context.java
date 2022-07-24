package async.commission.entity;

import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.HashMap;
import java.util.Map;

/**
 * 推演上下文
 */
@Data
public class Context {

    public Context() {

    }

    public Context(Map<String, Object> params) {
        this.params = params;
    }

    private Map<String, Object> params = new HashMap<String, Object>();

    /**
     * 获取参数
     *
     * @param key key值
     * @return 如果存在则返回对应条目值，否则返回null
     */
    public <T> T fetchParam(String key) {
        return (T) params.get(key);
    }

    /**
     * 增加参数
     *
     * @param key   key值
     * @param value value值
     */
    public void addParam(String key, Object value) {
        if (key != null && value != null) {
            this.params.put(key, value);
        }
    }

    /**
     * Getter method for property <tt>params</tt>
     *
     * @return property value of params
     */
    public Map<String, Object> getParams() {
        return params;
    }

    /**
     * Setter method for property <tt>params</tt>
     *
     * @param params value to be assigned to property code
     */
    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}

