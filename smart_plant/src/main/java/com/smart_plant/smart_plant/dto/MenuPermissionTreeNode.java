package com.smart_plant.smart_plant.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MenuPermissionTreeNode {

    private Long id;
    private Long parentId;
    private String permissionName;
    private String permissionCode;
    private String path;
    private Integer sort;
    private List<MenuPermissionTreeNode> children = new ArrayList<>();
}
