package com.cxxx.devnest.cmsweb.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.collection.CollUtil;
import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cxxx.devnest.admin.api.entity.SysDictItem;
import com.cxxx.devnest.cmsweb.api.constant.CertificationStatus;
import com.cxxx.devnest.cmsweb.api.entity.CmsEmployeeManagementEntity;
import com.cxxx.devnest.cmsweb.api.vo.EmployeeExportVO;
import com.cxxx.devnest.cmsweb.handler.MultiLevelCascadeHandler;
import com.cxxx.devnest.cmsweb.service.CmsEnterpriseInfoService;
import com.cxxx.devnest.cmsweb.service.CmsIndividualInfoService;
import com.cxxx.devnest.cmsweb.tools.CascadeData;
import com.cxxx.devnest.cmsweb.tools.DictTools;
import com.cxxx.devnest.common.core.util.R;
import com.cxxx.devnest.common.log.annotation.SysLog;
import com.cxxx.devnest.common.excel.annotation.ResponseExcel;
import com.cxxx.devnest.common.excel.annotation.RequestExcel;
import com.cxxx.devnest.cmsweb.service.CmsEmployeeManagementService;
import com.cxxx.devnest.cmsweb.api.vo.EmployeeImportVO;
import com.cxxx.devnest.cmsweb.api.entity.CmsIndividualInfoEntity;
import com.cxxx.devnest.cmsweb.api.entity.CmsEnterpriseInfoEntity;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.validation.BindingResult;

import java.io.IOException;
import java.net.URLEncoder;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import com.cxxx.devnest.common.security.annotation.HasPermission;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

import com.cxxx.devnest.admin.api.feign.RemoteDictService;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDate;

/**
 * 从业人员管理
 *
 * @author devnest
 * @date 2025-07-07 11:32:21
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/cmsEmployeeManagement" )
@Tag(description = "cmsEmployeeManagement" , name = "从业人员管理管理" )
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class CmsEmployeeManagementController {

    private final  CmsEmployeeManagementService cmsEmployeeManagementService;
    @Autowired
    private RemoteDictService remoteDictService;
    @Autowired
    private CmsIndividualInfoService cmsIndividualInfoService;
    @Autowired
    private CmsEnterpriseInfoService cmsEnterpriseInfoService;

    /**
     * 分页查询
     * @param page 分页对象
     * @param cmsEmployeeManagement 从业人员管理
     * @return
     */
    @Operation(summary = "分页查询" , description = "分页查询" )
    @GetMapping("/getInfo" )
    @HasPermission("cmsweb_cmsEmployeeManagement_view")
    public R getCmsEmployeeManagementPage(@ParameterObject Page page, @ParameterObject CmsEmployeeManagementEntity cmsEmployeeManagement) {
        LambdaQueryWrapper<CmsEmployeeManagementEntity> wrapper = Wrappers.lambdaQuery();
        // 其他条件
		wrapper.like(Objects.nonNull(cmsEmployeeManagement.getOwner()),CmsEmployeeManagementEntity::getOwner,cmsEmployeeManagement.getOwner());
		wrapper.eq(StrUtil.isNotBlank(cmsEmployeeManagement.getIdNumber()),CmsEmployeeManagementEntity::getIdNumber,cmsEmployeeManagement.getIdNumber());
		wrapper.eq(StrUtil.isNotBlank(cmsEmployeeManagement.getDrivingLicense()),CmsEmployeeManagementEntity::getDrivingLicense,cmsEmployeeManagement.getDrivingLicense());
		wrapper.like(StrUtil.isNotBlank(cmsEmployeeManagement.getVehicleClass()),CmsEmployeeManagementEntity::getVehicleClass,cmsEmployeeManagement.getVehicleClass());
		wrapper.like(StrUtil.isNotBlank(cmsEmployeeManagement.getIssuingOrganizations()),CmsEmployeeManagementEntity::getIssuingOrganizations,cmsEmployeeManagement.getIssuingOrganizations());
		wrapper.eq(Objects.nonNull(cmsEmployeeManagement.getValidPeriodFrom()),CmsEmployeeManagementEntity::getValidPeriodFrom,cmsEmployeeManagement.getValidPeriodFrom());
		wrapper.eq(Objects.nonNull(cmsEmployeeManagement.getValidPeriodTo()),CmsEmployeeManagementEntity::getValidPeriodTo,cmsEmployeeManagement.getValidPeriodTo());
		wrapper.eq(StrUtil.isNotBlank(cmsEmployeeManagement.getQualificationCertificate()),CmsEmployeeManagementEntity::getQualificationCertificate,cmsEmployeeManagement.getQualificationCertificate());
		wrapper.like(StrUtil.isNotBlank(cmsEmployeeManagement.getTelephone()),CmsEmployeeManagementEntity::getTelephone,cmsEmployeeManagement.getTelephone());
		wrapper.eq(StrUtil.isNotBlank(cmsEmployeeManagement.getCertificationStatus()),CmsEmployeeManagementEntity::getCertificationStatus,cmsEmployeeManagement.getCertificationStatus());
        wrapper.orderByDesc(CmsEmployeeManagementEntity::getCreateTime);
        return R.ok(cmsEmployeeManagementService.page(page, wrapper));
    }


    /**
     * 通过条件查询从业人员管理
     * @param cmsEmployeeManagement 查询条件
     * @return R  对象列表
     */
    @Operation(summary = "通过条件查询" , description = "通过条件查询对象" )
    @GetMapping("/details" )
    @HasPermission("cmsweb_cmsEmployeeManagement_view")
    public R getDetails(@ParameterObject CmsEmployeeManagementEntity cmsEmployeeManagement) {
        return R.ok(cmsEmployeeManagementService.list(Wrappers.query(cmsEmployeeManagement)));
    }

    /**
     * 新增从业人员管理
     * @param cmsEmployeeManagement 从业人员管理
     * @return R
     */
    @Operation(summary = "新增从业人员管理" , description = "新增从业人员管理" )
    @SysLog("新增从业人员管理" )
    @PostMapping("/saveInfo")
    @HasPermission("cmsweb_cmsEmployeeManagement_add")
    public R save(@RequestBody CmsEmployeeManagementEntity cmsEmployeeManagement) {
        // 确保添加时清空ID，让数据库自动生成
        cmsEmployeeManagement.setId(null);
        cmsEmployeeManagement.setCertificationStatus(CertificationStatus.PENDING);
        return R.ok(cmsEmployeeManagementService.save(cmsEmployeeManagement));
    }

    /**
     * 修改从业人员管理
     * @param cmsEmployeeManagement 从业人员管理
     * @return R
     */
    @Operation(summary = "修改从业人员管理" , description = "修改从业人员管理" )
    @SysLog("修改从业人员管理" )
    @PutMapping("/updateInfo")
    @HasPermission("cmsweb_cmsEmployeeManagement_edit")
    public R updateById(@RequestBody CmsEmployeeManagementEntity cmsEmployeeManagement) {
        return R.ok(cmsEmployeeManagementService.updateById(cmsEmployeeManagement));
    }

    /**
     * 通过id删除从业人员管理
     * @param ids id列表
     * @return R
     */
    @Operation(summary = "通过id删除从业人员管理" , description = "通过id删除从业人员管理" )
    @SysLog("通过id删除从业人员管理" )
    @DeleteMapping("/removeInfo")
    @HasPermission("cmsweb_cmsEmployeeManagement_del")
    public R removeById(@RequestBody Long[] ids) {
        return R.ok(cmsEmployeeManagementService.removeBatchByIds(CollUtil.toList(ids)));
    }

    /**
     * 导出excel 表格
     * @param cmsEmployeeManagement 查询条件
     * @param ids 导出指定ID
     * @return excel 文件流
     */
    @ResponseExcel
    @GetMapping("/export")
    @HasPermission("cmsweb_cmsEmployeeManagement_export")
    public List<EmployeeExportVO> exportExcel(CmsEmployeeManagementEntity cmsEmployeeManagement, Long[] ids) {
        List<CmsEmployeeManagementEntity> entityList = cmsEmployeeManagementService.list(
            Wrappers.lambdaQuery(cmsEmployeeManagement)
                .in(ArrayUtil.isNotEmpty(ids), CmsEmployeeManagementEntity::getId, ids)
        );
        return entityList.stream().map(entity -> {
            EmployeeExportVO vo = new EmployeeExportVO();
            vo.setDriverName(entity.getDriverName());
            vo.setIdNumber(entity.getIdNumber());
            vo.setDrivingLicense(entity.getDrivingLicense());
            vo.setVehicleClass(entity.getVehicleClass());
            vo.setIssuingOrganizations(entity.getIssuingOrganizations());
            vo.setValidPeriodFrom(entity.getValidPeriodFrom() != null ? entity.getValidPeriodFrom().toString() : "");
            vo.setValidPeriodTo(entity.getValidPeriodTo() != null ? entity.getValidPeriodTo().toString() : "");
            vo.setQualificationCertificate(entity.getQualificationCertificate());
            vo.setEmploymentType(getLabelByTypeAndValue("employmen_type", entity.getEmploymentType()));
            vo.setTelephone(entity.getTelephone());
            // 认证状态字典转换
            vo.setCertificationStatus(getLabelByTypeAndValue("certification_status", entity.getCertificationStatus()));
            vo.setBlacklistStatus(entity.getBlacklistStatus() != null ? (entity.getBlacklistStatus() == 1 ? "是" : "否") : "");
            vo.setOwnerType(entity.getOwnerType() != null ? (entity.getOwnerType() == 1 ? "个人" : "企业") : "");
            vo.setOwnerCompany(getOwnerCompanyName(entity.getOwner(), entity.getOwnerType()));
            vo.setRemark(entity.getRemark());
            vo.setRejectReason(entity.getReason());
            return vo;
        }).collect(Collectors.toList());
    }
    // value转label（远程调用）
    private String getLabelByTypeAndValue(String type, String value) {
        SysDictItem query = new SysDictItem();
        query.setDictType(type);
        query.setItemValue(value);
        R<SysDictItem> dictItemR = remoteDictService.getDictItemDetailsPost(query);
        return dictItemR != null && dictItemR.getData() != null ? dictItemR.getData().getLabel() : "";
    }
    /**
     * 导入excel 表
     * @param bindingResult 错误信息列表
     * @return ok fail
     */
    @PostMapping("/import")
    public R importExcel(@RequestExcel(headRowNumber = 2) List<EmployeeImportVO> voList, BindingResult bindingResult) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy/M/d");
        List<EmployeeImportVO> filteredList = voList.stream()
            .filter(vo -> vo != null && vo.getDriverName() != null && !vo.getDriverName().trim().isEmpty())
            .collect(Collectors.toList());
        List<CmsEmployeeManagementEntity> entityList = new ArrayList<>();
        for (EmployeeImportVO vo : filteredList) {
            CmsEmployeeManagementEntity entity = new CmsEmployeeManagementEntity();
            entity.setDriverName(vo.getDriverName());
            entity.setIdNumber(vo.getIdNumber());
            entity.setDrivingLicense(vo.getDrivingLicense());
            entity.setVehicleClass(vo.getVehicleClass());
            entity.setIssuingOrganizations(vo.getIssuingOrganizations());
            // 有效期自
            String validFromStr = vo.getValidPeriodFrom();
            if (validFromStr == null || validFromStr.trim().isEmpty()) {
                throw new IllegalArgumentException("Excel有效期自不能为空！");
            }
            entity.setValidPeriodFrom(LocalDate.parse(validFromStr.trim(), dateFormatter));
            // 有效期至
            String validToStr = vo.getValidPeriodTo();
            if (validToStr == null || validToStr.trim().isEmpty()) {
                throw new IllegalArgumentException("Excel有效期至不能为空！");
            }
            entity.setValidPeriodTo(LocalDate.parse(validToStr.trim(), dateFormatter));
            String extractLabel = extractLabel(vo.getEmploymentType());
            String gasolineValue = getDictValueByLabel("employmen_type", extractLabel);
            entity.setEmploymentType(gasolineValue);
            entity.setTelephone(vo.getTelephone());
            // 归属人类型（如“个体户_1”→1，“企业_2”→2）
            Integer ownerType = parseOwnerType(vo.getOwnerType());
            entity.setOwnerType(ownerType);
            // 归属企业/个体户名称查ID，查不到记录警告日志但不抛异常
            Long ownerId = findOwnerIdByName(vo.getOwnerCompany(), ownerType);
            if (ownerId == null) {
                log.warn("归属企业/个体户不存在：{}，ownerType={}", vo.getOwnerCompany(), ownerType);
            }
            entity.setOwner(ownerId);
            // 黑名单状态（如“是”→1，“否”→0）
            entity.setBlacklistStatus(parseBlacklistStatus(vo.getBlacklistStatus()));
            entity.setRemark(vo.getRemark());
            entity.setCertificationStatus(CertificationStatus.PENDING);
            entityList.add(entity);
        }
        cmsEmployeeManagementService.saveBatch(entityList);
        return R.ok("导入成功：" + entityList.size() + "条");
    }
    // 全部通过
    @Operation(summary = "全部通过", description = "将所有待审核装卸点全部通过认证")
    @PutMapping("/approveAll")
    public R approveAll() {
        int count = Math.toIntExact(cmsEmployeeManagementService.count(
            Wrappers.<CmsEmployeeManagementEntity>lambdaQuery()
                .eq(CmsEmployeeManagementEntity::getCertificationStatus, CertificationStatus.PENDING)
        ));
        if (count == 0) {
            return R.ok("没有待审核的数据，无需操作");
        }
        boolean result = cmsEmployeeManagementService.update(
                Wrappers.<CmsEmployeeManagementEntity>lambdaUpdate()
                        .eq(CmsEmployeeManagementEntity::getCertificationStatus, CertificationStatus.PENDING)
                        .set(CmsEmployeeManagementEntity::getCertificationStatus, CertificationStatus.APPROVED)
        );
        return result ? R.ok("全部通过成功") : R.failed("操作失败");
    }

    // 全部拒绝
    @Operation(summary = "全部拒绝", description = "将所有待审核装卸点全部拒绝认证")
    @PutMapping("/rejectAll")
    public R rejectAll(@RequestParam String reason) {
        int count = Math.toIntExact(cmsEmployeeManagementService.count(
                Wrappers.<CmsEmployeeManagementEntity>lambdaQuery()
                        .eq(CmsEmployeeManagementEntity::getCertificationStatus, CertificationStatus.PENDING)
        ));
        if (count == 0) {
            return R.ok("没有待审核的数据，无需操作");
        }
        boolean result = cmsEmployeeManagementService.update(
                Wrappers.<CmsEmployeeManagementEntity>lambdaUpdate()
                        .eq(CmsEmployeeManagementEntity::getCertificationStatus, CertificationStatus.PENDING)
                        .set(CmsEmployeeManagementEntity::getCertificationStatus, CertificationStatus.REJECTED)
                        .set(CmsEmployeeManagementEntity::getReason, reason)
        );
        return result ? R.ok("全部拒绝成功") : R.failed("操作失败");
    }
    // 工具方法
    private String extractLabel(String value) {
        if (value == null) {
            return null;
        }
        int idx = value.indexOf(':');
        if (idx > 0) {
            return value.substring(0, idx);
        }
        return value;
    }

    // 归属人类型字符串转数字
    private Integer parseOwnerType(String ownerType) {
        if (ownerType == null) {
            return null;
        }
        if (ownerType.contains("个体户")) {
            return 1;
        }
        if (ownerType.contains("企业")) {
            return 2;
        }
        return null;
    }

    // 归属企业/个体户名称查ID，查不到返回null
    private Long findOwnerIdByName(String name, Integer ownerType) {
        if (ownerType == null || name == null) {
            return null;
        }
        if (ownerType == 1) {
            CmsIndividualInfoEntity individual = cmsIndividualInfoService.lambdaQuery()
                .eq(CmsIndividualInfoEntity::getLegalName, name)
                .one();
            return individual != null ? individual.getId() : null;
        } else if (ownerType == 2) {
            CmsEnterpriseInfoEntity enterprise = cmsEnterpriseInfoService.lambdaQuery()
                .eq(CmsEnterpriseInfoEntity::getLegalName, name)
                .one();
            return enterprise != null ? enterprise.getId() : null;
        }
        return null;
    }

    // 黑名单状态字符串转数字
    private Integer parseBlacklistStatus(String status) {
        if ("是".equals(status)) {
            return 1;
        }
        if ("否".equals(status)) {
            return 0;
        }
        return null;
    }

    /**
     * 根据 ownerId 和 ownerType 查询所有人名称
     */
    private String getOwnerCompanyName(Long ownerId, Integer ownerType) {
        if (ownerId == null || ownerType == null) {
            return "";
        }
        if (ownerType == 1) {
            CmsIndividualInfoEntity individual = cmsIndividualInfoService.getById(ownerId);
            return individual != null ? individual.getLegalName() : "";
        } else if (ownerType == 2) {
            CmsEnterpriseInfoEntity enterprise = cmsEnterpriseInfoService.getById(ownerId);
            return enterprise != null ? enterprise.getLegalName() : "";
        }
        return "";
    }
    private String getDictValueByLabel(String dictType, String label) {
        R<List<SysDictItem>> result = remoteDictService.getDictByType(dictType);
        if (result != null && result.getData() != null) {
            for (SysDictItem item : result.getData()) {
                if (label.equals(item.getLabel())) {
                    return item.getItemValue();
                }
            }
        }
        return null;
    }


    /**
     * 导入模板
     */
    @GetMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response) throws IOException {

        // 设置响应头信息
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");

        // 文件名编码，防止中文乱码
        String fileName = URLEncoder.encode("从业人员导入模板", "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        Map<Integer, String[]> baseDictMap = new HashMap<>();
        CascadeData cascadeData = new CascadeData();


        List<SysDictItem> employmenType = remoteDictService.getDictByType("employmen_type").getData();
        baseDictMap.put(8, DictTools.getExcelEnum(employmenType));
        String[] blackSate = {"是:true","否:false"};
        baseDictMap.put(12, blackSate);

        String[] menType = {"个体户_1","企业_2"};
        baseDictMap.put(10, menType);

        //个体户信息
        LambdaQueryWrapper<CmsIndividualInfoEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(CmsIndividualInfoEntity::getCertificationStatus, "1");
        List<CmsIndividualInfoEntity> individualInfoEntities = cmsIndividualInfoService.getBaseMapper().selectList(wrapper);
        if (individualInfoEntities != null && individualInfoEntities.size() > 0) {
            String[] indiv = individualInfoEntities.stream()
                    .map(entity -> {
                        // 处理可能的null值，避免拼接出"null"字符串
                        String contact = entity.getContact() == null ? "" : entity.getContact();
                        Long id = entity.getId();
                        String idStr = id == null ? "" : id.toString();

                        // 拼接联系方式和ID，中间可以根据需要添加分隔符
                        return contact + ":" + idStr; // 这里用下划线作为分隔符，可根据需求修改
                    })
                    .toArray(String[]::new);

            cascadeData.addCascadeRelation(10, menType[0], 11, indiv);
        }

        //企业信息
        List<CmsEnterpriseInfoEntity> enterpriseInfoEntities = cmsEnterpriseInfoService.queryBycertificationStatus("1");
        if (enterpriseInfoEntities != null && enterpriseInfoEntities.size() > 0) {
            String[] enterprise = enterpriseInfoEntities.stream()
                    .map(entity -> {
                        // 处理可能的null值，避免拼接出"null"字符串
                        String enterpriseName = entity.getEnterpriseName() == null ? "" : entity.getEnterpriseName();
                        Long id = entity.getId();
                        String idStr = id == null ? "" : id.toString();

                        // 拼接联系方式和ID，中间可以根据需要添加分隔符
                        return enterpriseName + ":" + idStr; // 这里用下划线作为分隔符，可根据需求修改
                    })
                    .toArray(String[]::new);
            cascadeData.addCascadeRelation(10, menType[1], 11, enterprise);
        }


        // 7. 生成Excel模板
        EasyExcel.write(response.getOutputStream(), EmployeeImportVO.class)
                .sheet("从业人员导入模板")
                .registerWriteHandler(new MultiLevelCascadeHandler(baseDictMap, cascadeData))
                .doWrite(() -> {
                    // 返回空列表，只生成表头和模板样式
                    return null;
                });
    }
}

