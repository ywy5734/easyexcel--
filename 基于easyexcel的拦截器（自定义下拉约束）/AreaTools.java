package com.cxxx.devnest.cmsweb.tools;


import com.alibaba.cloud.commons.io.IOUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.cxxx.devnest.cmsweb.api.vo.Area;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 地区数据处理工具类
 */
public class AreaTools {

    // 省市区JSON文件默认路径
    private static final String DEFAULT_AREA_JSON_PATH = "省市区.txt";

    /**
     * 从默认路径读取省市区JSON数据
     */
    public static List<Area> readDefaultAreaJson() {
        return readAreaJsonFromResource(DEFAULT_AREA_JSON_PATH);
    }

    /**
     * 从指定资源路径读取省市区JSON数据
     * @param resourcePath 资源文件路径（相对于classpath）
     */
    public static List<Area> readAreaJsonFromResource(String resourcePath) {
        try {
            // 读取资源文件
            Resource resource = new ClassPathResource(resourcePath);
            try (InputStream inputStream = resource.getInputStream()) {
                String jsonContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
                return parseAreaJson(jsonContent);
            }
        } catch (IOException e) {
            throw new RuntimeException("读取省市区数据失败，路径：" + resourcePath, e);
        }
    }

    /**
     * 解析省市区JSON字符串
     */
    public static List<Area> parseAreaJson(String jsonContent) {
        return JSON.parseObject(jsonContent, new TypeReference<List<Area>>() {});
    }

    /**
     * 构建省市区级联关系
     * @param provinces 省份列表
     * @param cascadeData 级联数据对象
     * @param provinceColumn 省份列索引
     * @param cityColumn 城市列索引
     * @param districtColumn 区/县列索引
     */
    public static void buildAreaCascadeRelations(List<Area> provinces, CascadeData cascadeData,
                                                 int provinceColumn, int cityColumn, int districtColumn) {

        // 2. 构建省->市的级联关系
        for (Area province : provinces) {
            String provinceValue = buildAreaValue(province);

            // 获取该省份下的所有城市
            List<Area> cities = province.getChildren();
            if (cities != null && !cities.isEmpty()) {
                String[] cityValues = cities.stream()
                        .map(area -> buildAreaValue(area))
                        .toArray(String[]::new);

                // 添加省到市的级联关系
                cascadeData.addCascadeRelation(provinceColumn, provinceValue, cityColumn, cityValues);

                // 3. 构建市->区的级联关系
                for (Area city : cities) {
                    String cityValue = buildAreaValue(city);

                    // 获取该城市下的所有区/县
                    List<Area> districts = city.getChildren();
                    if (districts != null && !districts.isEmpty()) {
                        String[] districtValues = districts.stream()
                                .map(area -> buildAreaValue(area))
                                .toArray(String[]::new);

                        // 添加市到区的级联关系
                        cascadeData.addCascadeRelation(cityColumn, cityValue, districtColumn, districtValues);
                    }
                }
            }
        }
    }

    /**
     * 构建地区值，格式为"名称:编码"
     */
    public static String buildAreaValue(Area area) {
        return area.getLabel();
    }
}