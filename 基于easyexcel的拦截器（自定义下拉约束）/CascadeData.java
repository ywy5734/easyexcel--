package com.cxxx.devnest.cmsweb.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 多级级联数据结构辅助类
 */
public class CascadeData {
    // 级联层级结构: 父列索引 -> (父值 -> 子列信息)
    private Map<Integer, Map<String, List<CascadeItem>>> cascadeHierarchy = new HashMap<>();

    /**
     * 添加级联关系
     * @param parentColumn 父列索引
     * @param parentValue 父列值
     * @param childColumn 子列索引
     * @param childValues 子列可选值
     */
    public void addCascadeRelation(int parentColumn, String parentValue, int childColumn, String[] childValues) {
        // 确保父列对应的Map存在
        cascadeHierarchy.computeIfAbsent(parentColumn, k -> new HashMap<>());
        Map<String, List<CascadeItem>> parentValueMap = cascadeHierarchy.get(parentColumn);

        // 确保父值对应的List存在
        parentValueMap.computeIfAbsent(parentValue, k -> new ArrayList<>());

        // 添加子列信息
        parentValueMap.get(parentValue).add(new CascadeItem(childColumn, childValues));
    }

    /**
     * 获取父列对应的所有级联关系
     */
    public Map<String, List<CascadeItem>> getCascadeRelations(int parentColumn) {
        return cascadeHierarchy.getOrDefault(parentColumn, new HashMap<>());
    }

    /**
     * 级联子项信息
     */
    public static class CascadeItem {
        private int column;          // 子列索引
        private String[] values;     // 子列可选值

        public CascadeItem(int column, String[] values) {
            this.column = column;
            this.values = values;
        }

        public int getColumn() {
            return column;
        }

        public String[] getValues() {
            return values;
        }
    }

    public Map<Integer, Map<String, List<CascadeItem>>> getCascadeHierarchy() {
        return cascadeHierarchy;
    }
}

