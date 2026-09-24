package com.smart_plant.smart_plant.typehandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * 在 MySQL JSON 数组与 Java 字符串列表之间转换。
 *
 * <p>该处理器只负责持久化格式转换，业务层仍可直接使用 List&lt;String&gt;，
 * 从而让 REST 接口自然输出 JSON 数组而不是需要前端二次解析的字符串。</p>
 */
public class JsonStringListTypeHandler extends BaseTypeHandler<List<String>> {

    /** Jackson 线程安全实例，用于序列化和反序列化图片地址数组。 */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** 保留 List<String> 泛型信息，避免反序列化为无类型集合。 */
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() { };

    /** 将 Java 图片地址列表写入 MySQL JSON 列。 */
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<String> parameter, JdbcType jdbcType)
            throws SQLException {
        try {
            ps.setString(i, OBJECT_MAPPER.writeValueAsString(parameter));
        } catch (JsonProcessingException exception) {
            throw new SQLException("病虫害详情图片 JSON 序列化失败", exception);
        }
    }

    /** 从普通查询结果读取 JSON 数组。 */
    @Override
    public List<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName));
    }

    /** 从按列序号访问的查询结果读取 JSON 数组。 */
    @Override
    public List<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex));
    }

    /** 从存储过程结果读取 JSON 数组。 */
    @Override
    public List<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex));
    }

    /** 将数据库 JSON 文本安全解析为图片地址列表。 */
    private List<String> parse(String json) throws SQLException {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return OBJECT_MAPPER.readValue(json, STRING_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            throw new SQLException("病虫害详情图片 JSON 解析失败", exception);
        }
    }
}
