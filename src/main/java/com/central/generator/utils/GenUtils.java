package com.central.generator.utils;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import cn.hutool.core.date.DateUtil;
import com.central.generator.model.ColumnEntity;
import com.central.generator.model.CommonConstant;
import com.central.generator.model.TableEntity;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.commons.io.IOUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

/**
 * 代码生成器工具类
 *
 * @author zlt
 */
@Slf4j
public class GenUtils {
    private GenUtils() {
        throw new IllegalStateException("Utility class");
    }

    private final static String FILE_NAME_CONTROLLER = "Controller.java.vm";
    private final static String FILE_NAME_IService = "IService.java.vm";
    private final static String FILE_NAME_Service = "ServiceImpl.java.vm";
    private final static String FILE_NAME_IRepo = "Mapper.java.vm";
    private final static String FILE_NAME_XML = "Mapper.xml.vm";
    private final static String FILE_NAME_MODEL = "Model.java.vm";
    private final static String FILE_NAME_PARAM = "Param.java.vm";
    private final static String FILE_NAME_PARAM_VO = "ParamVo.java.vm";
    private final static String FILE_NAME_BUILDER = "Builder.java.vm";
    private final static String TEMPLATE_PATH = "template/";
    private final static String PACKAGE = "package";
    private final static String MODULE_NAME = "moduleName";

    public static List<String> getTemplates() {
        List<String> templates = new ArrayList<>();
        templates.add(TEMPLATE_PATH + FILE_NAME_CONTROLLER);
        templates.add(TEMPLATE_PATH + FILE_NAME_MODEL);
        templates.add(TEMPLATE_PATH + FILE_NAME_IRepo);
        templates.add(TEMPLATE_PATH + FILE_NAME_XML);
        templates.add(TEMPLATE_PATH + FILE_NAME_IService);
        templates.add(TEMPLATE_PATH + FILE_NAME_Service);
        templates.add(TEMPLATE_PATH + FILE_NAME_PARAM);
        templates.add(TEMPLATE_PATH + FILE_NAME_PARAM_VO);
        templates.add(TEMPLATE_PATH + FILE_NAME_BUILDER);
        return templates;
    }

    /**
     * 生成代码
     */
    public static void generatorCode(Map<String, String> table,
                                     List<Map<String, String>> columns, ZipOutputStream zip) {
        //配置信息
        Configuration config = getConfig();
        boolean hasBigDecimal = false;
        boolean hasDate = false;
        //表信息
        TableEntity tableEntity = new TableEntity();
        tableEntity.setTableName(table.get("tableName"));
        tableEntity.setComments(table.get("tableComment"));
        //表名转换成Java类名
        String className = tableToJava(tableEntity.getTableName(), config.getString("tablePrefix"));
        String lTablePrefix = className.substring(0, 3).toLowerCase();
        tableEntity.setClassName(className);
        tableEntity.setClassname(StringUtils.uncapitalize(className));

        //列信息
        List<ColumnEntity> columsList = new ArrayList<>();
        int index = 0;
        for (Map<String, String> column : columns) {
            index++;
            ColumnEntity columnEntity = new ColumnEntity();
            columnEntity.setColumnName(column.get("columnName"));
            columnEntity.setDataType(column.get("dataType"));
            columnEntity.setComments(column.get("columnComment"));
            columnEntity.setExtra(column.get("extra"));

            //列名转换成Java属性名
            String attrName = columnToJava(columnEntity.getColumnName());
            columnEntity.setAttrName(attrName);
            columnEntity.setAttrname(StringUtils.uncapitalize(attrName));
            columnEntity.setLast(index == columns.size()?1:0);
            //列的数据类型，转换成Java类型
            String attrType = config.getString(columnEntity.getDataType(), "unknowType");
            columnEntity.setAttrType(attrType);
            if (!hasBigDecimal && attrType.equals("BigDecimal")) {
                hasBigDecimal = true;
            }
            if (!hasDate && attrType.equals("LocalDate") && !"create_time".equals(columnEntity.getColumnName()) && !"update_time".equals(columnEntity.getColumnName())) {
                hasDate = true;
            }
            //是否主键
            if ("PRI".equalsIgnoreCase(column.get("columnKey")) && tableEntity.getPk() == null) {
                tableEntity.setPk(columnEntity);
            }
            columsList.add(columnEntity);
        }
        tableEntity.setColumns(columsList);

        //没主键，则第一个字段为主键
        if (tableEntity.getPk() == null) {
            tableEntity.setPk(tableEntity.getColumns().get(0));
        }

        //设置velocity资源加载器
        Properties prop = new Properties();
        prop.put("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        Velocity.init(prop);
        String mainPath = config.getString("mainPath");
        mainPath = StringUtils.isBlank(mainPath) ? "io.renren" : mainPath;
        //封装模板数据
        Map<String, Object> map = new HashMap<>();
        map.put("tableName", tableEntity.getTableName());
        map.put("lTablePrefix", lTablePrefix);
        map.put("comments", tableEntity.getComments());
        map.put("pk", tableEntity.getPk());
        map.put("className", tableEntity.getClassName());
        map.put("classname", tableEntity.getClassname());
        map.put("pathName", tableEntity.getClassname().toLowerCase());
        map.put("columns", tableEntity.getColumns());
        map.put("hasBigDecimal", hasBigDecimal);
        map.put("hasDate", hasDate);
        map.put("mainPath", mainPath);
        map.put(PACKAGE, config.getString(PACKAGE));
        map.put(MODULE_NAME, config.getString(MODULE_NAME));
        map.put("author", config.getString("author"));
        map.put("email", config.getString("email"));
        map.put("datetime", DateUtil.format(new Date(), CommonConstant.DATETIME_FORMAT));
        VelocityContext context = new VelocityContext(map);

        //获取模板列表
        List<String> templates = getTemplates();
        for (String template : templates) {
            //渲染模板
            try (
                    StringWriter sw = new StringWriter()
            ) {
                Template tpl = Velocity.getTemplate(template, "UTF-8");
                tpl.merge(context, sw);

                //添加到zip
                zip.putNextEntry(new ZipEntry(getFileName(template, tableEntity.getClassName(), config.getString(PACKAGE), config.getString(MODULE_NAME))));
                IOUtils.write(sw.toString(), zip, StandardCharsets.UTF_8);
                zip.closeEntry();
            } catch (IOException e) {
                log.error("generatorCode-error", e);
            }
        }
    }

    /**
     * 列名转换成Java属性名
     */
    public static String columnToJava(String columnName) {
        return WordUtils.capitalizeFully(columnName, new char[]{'_'}).replace("_", "");
    }

    /**
     * 表名转换成Java类名
     */
    public static String tableToJava(String tableName, String tablePrefix) {
        if (StringUtils.isNotBlank(tablePrefix)) {
            tableName = tableName.substring(tablePrefix.length());
        }
        return columnToJava(tableName);
    }

    /**
     * 获取配置信息
     */
    public static Configuration getConfig() {
        try {
            return new PropertiesConfiguration("generator.properties");
        } catch (ConfigurationException e) {
            throw new RuntimeException("获取配置文件失败，", e);
        }
    }

    /**
     * 获取文件名
     */
    public static String getFileName(String template, String className, String packageName, String moduleName) {
        String packagePath = "main" + File.separator + "java" + File.separator;
        String servicePackagePath = packagePath;
        String sharedPackagePath = packagePath;
        if (StringUtils.isNotBlank(packageName)) {
            packagePath += packageName.replace(".", File.separator) + File.separator + moduleName + File.separator;
            servicePackagePath += packageName.replace(".", File.separator) + File.separator + "service" + File.separator;
            sharedPackagePath += packageName.replace(".", File.separator) + File.separator + "shared" + File.separator;
        }
        if (template.contains(FILE_NAME_CONTROLLER)) {
            return packagePath + "controller" + File.separator + className + "Controller.java";
        }
        if (template.contains(FILE_NAME_IService)) {
            return servicePackagePath + File.separator + "api" + File.separator + "I" + className + "Service.java";
        }
        if (template.contains(FILE_NAME_Service)) {
            return servicePackagePath + File.separator + "impl" + File.separator + className + "ServiceImpl.java";
        }
        if (template.contains(FILE_NAME_IRepo)) {
            return servicePackagePath + File.separator + "repo" + File.separator + className + "Mapper.java";
        }
        if (template.contains(FILE_NAME_XML)) {
            return "main" + File.separator + "resources" + File.separator + "mapper" + File.separator + className + "Mapper.xml";
        }
        if (template.contains(FILE_NAME_MODEL)) {
            return sharedPackagePath + File.separator + "domain" + File.separator + "model" + File.separator + className + ".java";
        }
        if (template.contains(FILE_NAME_PARAM)) {
            return sharedPackagePath + File.separator + "domain" + File.separator + "param" + File.separator + className + "ListReq.java";
        }
        if (template.contains(FILE_NAME_PARAM_VO)) {
            return sharedPackagePath + File.separator + "domain" + File.separator + "vo" + File.separator + className + "ListVo.java";
        }
        if (template.contains(FILE_NAME_BUILDER)) {
            return sharedPackagePath + File.separator + "builder" + File.separator + className + "Builder.java";
        }
        return null;
    }
}
