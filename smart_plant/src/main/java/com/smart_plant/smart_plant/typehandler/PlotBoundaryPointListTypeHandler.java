package com.smart_plant.smart_plant.typehandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart_plant.smart_plant.dto.PlotBoundaryPoint;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * 在 MySQL JSON 字段与地块边界顶点列表之间转换。
 */
public class PlotBoundaryPointListTypeHandler extends BaseTypeHandler<List<PlotBoundaryPoint>> {

    /** Jackson 实例可安全复用，避免每次数据库读写重复创建解析器。 */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** 保留列表元素的具体类型，防止反序列化成无类型 Map。 */
    private static final TypeReference<List<PlotBoundaryPoint>> POINT_LIST_TYPE = new TypeReference<>() { };

    /** 将 Java 边界点列表序列化后写入 MySQL JSON 列。 */
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<PlotBoundaryPoint> parameter, JdbcType jdbcType)
            throws SQLException {
        try {
            ps.setString(i, OBJECT_MAPPER.writeValueAsString(parameter));
        } catch (JsonProcessingException exception) {
            throw new SQLException("地块边界 JSON 序列化失败", exception);
        }
    }

    /** 按列名读取并解析地块边界。 */
    @Override
    public List<PlotBoundaryPoint> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName));
    }

    /** 按列序号读取并解析地块边界。 */
    @Override
    public List<PlotBoundaryPoint> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex));
    }

    /** 从存储过程结果读取并解析地块边界。 */
    @Override
    public List<PlotBoundaryPoint> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex));
    }

    /** 空数据库值返回空列表，让 REST 响应保持稳定的数组结构。 */
    private List<PlotBoundaryPoint> parse(String json) throws SQLException {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return OBJECT_MAPPER.readValue(json, POINT_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            throw new SQLException("地块边界 JSON 解析失败", exception);
        }
    }
}
