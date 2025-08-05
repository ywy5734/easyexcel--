package com.cxxx.devnest.cmsweb.handler;

import com.alibaba.excel.write.handler.SheetWriteHandler;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.metadata.holder.WriteWorkbookHolder;
import com.cxxx.devnest.cmsweb.tools.CascadeData;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFDataValidation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通用Excel下拉框处理器，自动处理长列表，支持多级级联
 */
public class MultiLevelCascadeHandler implements SheetWriteHandler {

    // 基础字典数据：列索引 -> 可选值数组
    private final Map<Integer, String[]> baseDictMap;

    // 级联数据结构
    private final CascadeData cascadeData;

    // 存储每个sheet的名称，避免重复
    private final Map<String, String> sheetNameMap = new HashMap<>();

    // Excel显式下拉列表的最大字符限制
    private static final int EXCEL_EXPLICIT_LIST_MAX_LENGTH = 255;

    public MultiLevelCascadeHandler(Map<Integer, String[]> baseDictMap, CascadeData cascadeData) {
        this.baseDictMap = baseDictMap;
        this.cascadeData = cascadeData;
    }

    @Override
    public void beforeSheetCreate(WriteWorkbookHolder writeWorkbookHolder, WriteSheetHolder writeSheetHolder) {
        // 创建所有级联和长列表需要的隐藏sheet
        createHiddenSheets(writeWorkbookHolder.getWorkbook());
    }


    @Override
    public void afterSheetCreate(WriteWorkbookHolder writeWorkbookHolder, WriteSheetHolder writeSheetHolder) {
        Sheet mainSheet = writeSheetHolder.getSheet();
        DataValidationHelper helper = mainSheet.getDataValidationHelper();

        // 设置基础下拉框（自动处理长列表）
        setupBaseDropdowns(mainSheet, helper);

        // 设置所有级联关系（支持多级）
        setupAllCascades(mainSheet, helper);

        // 隐藏所有辅助sheet
        hideHelperSheets(writeWorkbookHolder.getWorkbook());
    }

    /**
     * 创建所有需要的隐藏sheet（包括级联和长列表）
     */
    private void createHiddenSheets(Workbook workbook) {
        // 创建级联需要的隐藏sheet
        createCascadeSheets(workbook);

        // 创建长列表需要的隐藏sheet
        createLongListSheets(workbook);
    }

    /**
     * 创建级联需要的隐藏sheet
     */
    private void createCascadeSheets(Workbook workbook) {
        if (cascadeData == null || cascadeData.getCascadeHierarchy().isEmpty()) {
            return;
        }

        for (Map.Entry<Integer, Map<String, List<CascadeData.CascadeItem>>> entry :
                cascadeData.getCascadeHierarchy().entrySet()) {

            int parentColumn = entry.getKey();
            Map<String, List<CascadeData.CascadeItem>> parentValueMap = entry.getValue();

            String sheetName = getUniqueSheetName("cascade_" + parentColumn);
            sheetNameMap.put(parentColumn + "", sheetName);
            Sheet sheet = workbook.createSheet(sheetName);
            workbook.setSheetHidden(workbook.getSheetIndex(sheet), false);

            int rowIndex = 0;
            // 遍历父值
            for (Map.Entry<String, List<CascadeData.CascadeItem>> valueEntry : parentValueMap.entrySet()) {
                String parentValue = valueEntry.getKey();
                List<CascadeData.CascadeItem> childItems = valueEntry.getValue();

                // 遍历子列信息
                for (CascadeData.CascadeItem item : childItems) {
                    String[] childValues = item.getValues();
                    // 纵向写入子选项：A列存父值，B列存子选项
                    for (String childValue : childValues) {
                        Row row = sheet.createRow(rowIndex++);
                        row.createCell(0).setCellValue(parentValue);
                        row.createCell(1).setCellValue(childValue);
                    }
                }
            }
        }
    }

    /**
     * 为长列表创建隐藏sheet
     */
    private void createLongListSheets(Workbook workbook) {
        if (baseDictMap == null || baseDictMap.isEmpty()) {
            return;
        }

        for (Map.Entry<Integer, String[]> entry : baseDictMap.entrySet()) {
            int column = entry.getKey();
            String[] values = entry.getValue();

            // 计算列表总长度，判断是否需要创建隐藏sheet
            if (calculateListTotalLength(values) > EXCEL_EXPLICIT_LIST_MAX_LENGTH) {
                String sheetName = getUniqueSheetName("list_" + column);
                sheetNameMap.put(column+"", sheetName);
                Sheet sheet = workbook.createSheet(sheetName);
                workbook.setSheetHidden(workbook.getSheetIndex(sheet), false);
                // 将选项写入隐藏sheet的A列
                for (int i = 0; i < values.length; i++) {
                    Row row = sheet.createRow(i);
                    row.createCell(0).setCellValue(values[i]);
                }
            }
        }
    }

    /**
     * 设置基础下拉框（自动处理长列表）
     */
    private void setupBaseDropdowns(Sheet sheet, DataValidationHelper helper) {
        if (baseDictMap == null || baseDictMap.isEmpty()) {
            return;
        }

        for (Map.Entry<Integer, String[]> entry : baseDictMap.entrySet()) {
            int column = entry.getKey();
            String[] values = entry.getValue();

            CellRangeAddressList addressList = new CellRangeAddressList(1, 1000, column, column);
            DataValidationConstraint constraint;

            // 检查列表长度，自动选择合适的方式
            if (calculateListTotalLength(values) > EXCEL_EXPLICIT_LIST_MAX_LENGTH) {
                // 长列表：使用隐藏sheet
                String sheetName = sheetNameMap.get(column+"");
                String formula = String.format("'%s'!$A$1:$A$%d", sheetName, values.length);
                constraint = helper.createFormulaListConstraint(formula);
            } else {
                // 短列表：使用显式列表
                constraint = helper.createExplicitListConstraint(values);
            }

            DataValidation validation = helper.createValidation(constraint, addressList);
            setupValidationProperties(validation);
            sheet.addValidationData(validation);
        }
    }

    /**
     * 计算列表所有元素拼接后的总长度（包含逗号分隔符）
     */
    private int calculateListTotalLength(String[] values) {
        if (values == null || values.length == 0) {
            return 0;
        }

        int totalLength = 0;
        for (String value : values) {
            totalLength += value.length();
        }
        // 加上元素之间的逗号数量 (n-1个)
        totalLength += (values.length - 1);

        return totalLength;
    }

    /**
     * 设置所有级联关系（支持多级）
     */
    private void setupAllCascades(Sheet sheet, DataValidationHelper helper) {
        if (cascadeData == null || cascadeData.getCascadeHierarchy().isEmpty()) {
            return;
        }

        // 处理第一级级联（二级菜单）
        for (Map.Entry<Integer, Map<String, List<CascadeData.CascadeItem>>> entry :
                cascadeData.getCascadeHierarchy().entrySet()) {

            int parentColumn = entry.getKey();
            setupCascadeForParentColumn(sheet, helper, parentColumn, 1);
        }
    }

    /**
     * 为指定父列设置级联下拉
     * @param level 级联层级（1表示二级，2表示三级）
     */
    private void setupCascadeForParentColumn(Sheet sheet, DataValidationHelper helper,
                                             int parentColumn, int level) {
        // 获取该父列的所有级联关系
        Map<String, List<CascadeData.CascadeItem>> cascadeRelations =
                cascadeData.getCascadeRelations(parentColumn);

        if (cascadeRelations.isEmpty()) {
            return;
        }

        // 获取该父列对应的隐藏sheet名称
        String sheetName = sheetNameMap.get(parentColumn + "");
        if (sheetName == null) {
            return;
        }

        // 获取第一个父值对应的子列信息
        List<CascadeData.CascadeItem> firstChildItems =
                cascadeRelations.values().iterator().next();

        // 为每个子列设置级联下拉
        for (CascadeData.CascadeItem childItem : firstChildItems) {
            int childColumn = childItem.getColumn();

            // 创建子列的数据验证
            CellRangeAddressList addressList = new CellRangeAddressList(1, 1000, childColumn, childColumn);

            // 创建级联公式
            String formula = createCascadeFormula(parentColumn, sheetName);

            DataValidationConstraint constraint = helper.createFormulaListConstraint(formula);
            DataValidation validation = helper.createValidation(constraint, addressList);

            setupValidationProperties(validation);
            sheet.addValidationData(validation);

            // 如果需要支持三级级联，递归设置下一级
            if (level < 2) {  // 限制最大层级为3级（level从1开始）
                setupCascadeForParentColumn(sheet, helper, childColumn, level + 1);
            }
        }
    }

    /**
     * 创建级联公式
     */
    private String createCascadeFormula(int parentColumn, String sheetName) {
        String parentColumnLetter = getColumnLetter(parentColumn);
        // 公式逻辑：根据父值在隐藏Sheet的A列找到对应行，引用B列的子选项范围
        return String.format(
                "INDIRECT(\"%s!$B$\"&MATCH($%s2,%s!$A:$A,0)&\":$B$\"&" +
                        "(MATCH($%s2,%s!$A:$A,0)+COUNTIF(%s!$A:$A,$%s2)-1))",
                sheetName,
                parentColumnLetter, sheetName,  // MATCH定位父值在A列的起始行
                parentColumnLetter, sheetName,  // COUNTIF计算父值对应的子选项数量
                sheetName, parentColumnLetter
        );
    }

    /**
     * 设置验证的通用属性
     */
    private void setupValidationProperties(DataValidation validation) {
        validation.setShowErrorBox(true);
        validation.createErrorBox("输入错误", "请从下拉列表中选择有效值");

        if (validation instanceof XSSFDataValidation) {
            validation.setSuppressDropDownArrow(true);
        } else {
            validation.setSuppressDropDownArrow(false);
        }
    }

    /**
     * 生成唯一的sheet名称
     */
    private String getUniqueSheetName(String baseName) {
        String sheetName = baseName;
        int counter = 1;
        while (sheetNameMap.containsValue(sheetName)) {
            sheetName = baseName + "_" + counter++;
        }
        return sheetName;
    }

    /**
     * 将列索引转换为Excel列字母（如0->A, 1->B）
     */
    private String getColumnLetter(int columnIndex) {
        StringBuilder sb = new StringBuilder();
        int index = columnIndex;
        while (index >= 0) {
            sb.insert(0, (char) ('A' + index % 26));
            index = (index / 26) - 1;
        }
        return sb.toString();
    }

    /**
     * 隐藏所有辅助sheet
     */
    private void hideHelperSheets(Workbook workbook) {
        workbook.setActiveSheet(workbook.getNumberOfSheets()-1);// 确保激活第一个sheet

        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            String sheetName = workbook.getSheetName(i);

            // 隐藏所有辅助sheet（以cascade_或list_开头）
            if (sheetName.startsWith("cascade_") || sheetName.startsWith("list_")) {
                workbook.setSheetHidden(i, true);
            }else {
                Sheet sheet = workbook.getSheet(sheetName);
                int maxColumn = 20;
                // 为所有列设置默认宽度
                for (int j = 0; j <= maxColumn; j++) {
                    // 如果columnWidthMap中有单独设置，则优先使用单独设置的值
                    sheet.setColumnWidth(j, 25*256);
                }
            }
        }
    }

}

