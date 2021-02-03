package com.qbb.upload;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.qbb.constant.YapiConstant;
import com.qbb.dto.*;
import com.qbb.util.HttpClientUtil;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;

import java.io.File;
import java.io.IOException;
import java.util.*;


/**
 * 上传到yapi
 *
 * @author chengsheng@qbb6.com
 * @date 2019/1/31 11:41 AM
 */
public class UploadYapi {


    private Gson gson=new Gson();


    /**
     * @description: 调用保存接口
     * @param: [yapiSaveParam, attachUpload, path]
     * @return: com.qbb.dto.YapiResponse
     * @author: chengsheng@qbb6.com
     * @date: 2019/5/15
     */ 
    public YapiResponse  uploadSave(YapiSaveParam yapiSaveParam,String attachUpload,String path) throws IOException {
        if(Strings.isNullOrEmpty(yapiSaveParam.getTitle())){
            yapiSaveParam.setTitle(yapiSaveParam.getPath());
        }
        YapiHeaderDTO yapiHeaderDTO=new YapiHeaderDTO();
        if("form".equals(yapiSaveParam.getReq_body_type())){
            yapiHeaderDTO.setName("Content-Type");
            yapiHeaderDTO.setValue("application/x-www-form-urlencoded");
            yapiSaveParam.setReq_body_form(yapiSaveParam.getReq_body_form());
        }else{
            yapiHeaderDTO.setName("Content-Type");
            yapiHeaderDTO.setValue("application/json");
            yapiSaveParam.setReq_body_type("json");
        }
        if(Objects.isNull(yapiSaveParam.getReq_headers())){
            List list=new ArrayList();
            list.add(yapiHeaderDTO);
            yapiSaveParam.setReq_headers(list);
        }else{
            yapiSaveParam.getReq_headers().add(yapiHeaderDTO);
        }
        this.changeDesByPath(yapiSaveParam);
        YapiResponse yapiResponse= this.getCatIdOrCreate(yapiSaveParam);
        if(yapiResponse.getErrcode()==0){
            yapiSaveParam.setCatid(yapiResponse.getCatId());
            System.out.println(String.format("url: %s \n, para: %s \n", yapiSaveParam.getYapiUrl()+YapiConstant.yapiSave,  gson.toJson(yapiSaveParam)));

            String response=HttpClientUtil.ObjectToString(HttpClientUtil.getHttpclient().execute(this.getHttpPost(yapiSaveParam.getYapiUrl()+YapiConstant.yapiSave,gson.toJson(yapiSaveParam))),"utf-8");
            YapiResponse yapiResponseResult= gson.fromJson(response,YapiResponse.class);
            yapiResponseResult.setCatId(yapiSaveParam.getCatid());
            return yapiResponseResult;
        }else{
            return yapiResponse;
        }
    }



    /**
     * 获得httpPost
     * @return
     */
    private HttpPost getHttpPost(String url, String body) {
        HttpPost httpPost = null;
        try {
            httpPost = new HttpPost(url);
            httpPost.setHeader("Content-type", "application/json;charset=utf-8");
            HttpEntity reqEntity = new StringEntity(body == null ? "" : body, "UTF-8");
            httpPost.setEntity(reqEntity);
        } catch (Exception e) {
        }
        return httpPost;
    }

    /**
     * @description: 上传文件
     * @param: [url, filePath]
     * @return: java.lang.String
     * @author: chengsheng@qbb6.com
     * @date: 2019/5/15
     */ 
    public String uploadFile(String url,String filePath){
        HttpPost httpPost = null;
        try {
            httpPost = new HttpPost(url);
            FileBody bin = new FileBody(new File(filePath));
            HttpEntity reqEntity = MultipartEntityBuilder.create().addPart("file", bin).build();
            httpPost.setEntity(reqEntity);
            return  HttpClientUtil.ObjectToString(HttpClientUtil.getHttpclient().execute(httpPost),"utf-8");
        } catch (Exception e) {
        }
        return "";
    }


    private HttpGet getHttpGet(String url){
        try {
            return HttpClientUtil.getHttpGet(url, "application/json", "application/json; charset=utf-8");
        } catch (IOException e) {
        }
        return null;
    }
    /**
     * @description: 获得描述
     * @param: [yapiSaveParam]
     * @return: com.qbb.dto.YapiResponse
     * @author: chengsheng@qbb6.com
     * @date: 2019/7/28
     */ 
    public void changeDesByPath(YapiSaveParam yapiSaveParam){
        try{
            String response = HttpClientUtil.ObjectToString(HttpClientUtil.getHttpclient().execute(this.getHttpGet(yapiSaveParam.getYapiUrl()+ YapiConstant.yapiGetByPath+"?token="+yapiSaveParam.getToken()+"&path="+yapiSaveParam.getPath())),"utf-8");
            YapiResponse yapiResponse=gson.fromJson(response,YapiResponse.class);
            if(yapiResponse.getErrcode()==0) {
                YapiInterfaceResponse yapiInterfaceResponse=gson.fromJson(gson.toJson(yapiResponse.getData()),YapiInterfaceResponse.class);
                if(!Strings.isNullOrEmpty(yapiInterfaceResponse.getDesc())){
                    //如果原来描述不为空，那么就将当前描述+上一个版本描述的自定义部分
                    if(yapiInterfaceResponse.getDesc().contains("java类")){
                        yapiSaveParam.setDesc(yapiInterfaceResponse.getDesc().substring(0,yapiInterfaceResponse.getDesc().indexOf("java类"))+yapiSaveParam.getDesc()+yapiInterfaceResponse.getDesc().substring(yapiInterfaceResponse.getDesc().indexOf("</pre>"),yapiInterfaceResponse.getDesc().length()));
                    }else{
                        yapiSaveParam.setDesc(yapiInterfaceResponse.getDesc().substring(0,yapiInterfaceResponse.getDesc().indexOf("<pre>"))+yapiSaveParam.getDesc()+yapiInterfaceResponse.getDesc().substring(yapiInterfaceResponse.getDesc().indexOf("</pre>"),yapiInterfaceResponse.getDesc().length()));
                    }
                }
                if(Objects.nonNull(yapiInterfaceResponse.getCatid())){
                    yapiSaveParam.setCatid(yapiInterfaceResponse.getCatid().toString());
                }
            }
        }catch (Exception e){

        }
    }

    /**
     * @description: 获得分类或者创建分类或者
     * @param: [yapiSaveParam]
     * @return: com.qbb.dto.YapiResponse
     * @author: chengsheng@qbb6.com
     * @date: 2019/5/15
     */
    public YapiResponse getCatIdOrCreate(YapiSaveParam yapiSaveParam){
        YapiResponse res = new YapiResponse();

        // 如果缓存不存在，切自定义菜单为空，则使用默认目录
        if(Strings.isNullOrEmpty(yapiSaveParam.getMenu())){
            yapiSaveParam.setMenu(YapiConstant.menu);
        }
        try {
            String response = HttpClientUtil.ObjectToString(HttpClientUtil.getHttpclient().execute(this.getHttpGet(yapiSaveParam.getYapiUrl()+ YapiConstant.yapiCatMenu+"?project_id="+yapiSaveParam.getProjectId()+"&token="+yapiSaveParam.getToken())),"utf-8");
            YapiResponse yapiResponse=gson.fromJson(response,YapiResponse.class);
            if(yapiResponse.getErrcode()==0) {
                List<YapiCatResponse> list = (List<YapiCatResponse>) yapiResponse.getData();
                list=gson.fromJson(gson.toJson(list),new TypeToken<List<YapiCatResponse>>() {}.getType());
                String[] menus=yapiSaveParam.getMenu().split("/");
                int parentId = -1;
                for (String menu:menus) {
                    int finalParentId = parentId;
                    YapiCatResponse menuRes = list.stream()
                            .filter(item->item.getName().equalsIgnoreCase(menu) && item.getParent_id() == finalParentId)
                            .findFirst()
                            .orElse(null)
                            ;
                    if (menuRes == null) {
                        parentId = addMenu(yapiSaveParam, parentId, menu);
                    }else {
                        parentId = menuRes.get_id();
                    }
                }
                res.setCatId(parentId+"");
            }
            return  res;
        } catch (Exception e) {
            e.printStackTrace();
            try {
                //出现这种情况可能是yapi 版本不支持
                yapiSaveParam.setCatid(addMenu(yapiSaveParam,-1,yapiSaveParam.getMenu()).toString());
                return new YapiResponse();
            } catch (IOException e1) {
            }
            return  new YapiResponse(0,e.toString());
        }
    }

    public static void main(String[] args) {
        UploadYapi uploadYapi = new UploadYapi();

        YapiSaveParam param = new YapiSaveParam();
        param.setProjectId(216);
        param.setToken("7e504e0fddf5d568d34f2c9985de939a2b2666811ccd2a4ab88d22e7221ff060");
        param.setYapiUrl("https://yapi.ekuaibao.com/");
        param.setMenu("企业端/订单");
        YapiResponse res = uploadYapi.getCatIdOrCreate(param);

        System.out.println();
    }


    /**
     * @description: 新增菜单
     * @param: [yapiSaveParam, parent_id]
     * @return: java.lang.Integer
     * @author: chengsheng@qbb6.com
     * @date: 2019/7/28
     */ 
    private Integer addMenu(YapiSaveParam yapiSaveParam,Integer parent_id,String menu) throws IOException{
        YapiCatMenuParam  yapiCatMenuParam=new YapiCatMenuParam(menu,yapiSaveParam.getProjectId(),yapiSaveParam.getToken(),parent_id);
        String responseCat=HttpClientUtil.ObjectToString(HttpClientUtil.getHttpclient().execute(this.getHttpPost(yapiSaveParam.getYapiUrl()+YapiConstant.yapiAddCat,gson.toJson(yapiCatMenuParam))),"utf-8");
        YapiCatResponse yapiCatResponse=gson.fromJson(gson.fromJson(responseCat,YapiResponse.class).getData().toString(),YapiCatResponse.class);
        return yapiCatResponse.get_id();
    }


}
