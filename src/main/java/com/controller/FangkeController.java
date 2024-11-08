
package com.controller;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.text.SimpleDateFormat;
import com.alibaba.fastjson.JSONObject;
import java.util.*;
import org.springframework.beans.BeanUtils;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.context.ContextLoader;
import javax.servlet.ServletContext;
import com.service.TokenService;
import com.utils.*;
import java.lang.reflect.InvocationTargetException;

import com.service.DictionaryService;
import org.apache.commons.lang3.StringUtils;
import com.annotation.IgnoreAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.entity.*;
import com.entity.view.*;
import com.service.*;
import com.utils.PageUtils;
import com.utils.R;
import com.alibaba.fastjson.*;

/**
 * 访客信息
 * 后端接口
 * @author
 * @email
*/
@RestController
@Controller
@RequestMapping("/fangke")
public class FangkeController {
    private static final Logger logger = LoggerFactory.getLogger(FangkeController.class);

    private static final String TABLE_NAME = "fangke";

    @Autowired
    private FangkeService fangkeService;


    @Autowired
    private TokenService tokenService;
    @Autowired
    private DictionaryService dictionaryService;

    //级联表非注册的service
    //注册表service
    @Autowired
    private YonghuService yonghuService;
    @Autowired
    private JiashuService jiashuService;


    /**
    * 后端列表
    */
    @RequestMapping("/page")
    public R page(@RequestParam Map<String, Object> params, HttpServletRequest request){
        logger.debug("page方法:,,Controller:{},,params:{}",this.getClass().getName(),JSONObject.toJSONString(params));
        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(false)
            return R.error(511,"永不会进入");
        else if("老人".equals(role))
            params.put("yonghuId",request.getSession().getAttribute("userId"));
        else if("家属".equals(role))
            params.put("jiashuId",request.getSession().getAttribute("userId"));
        params.put("fangkeDeleteStart",1);params.put("fangkeDeleteEnd",1);
        CommonUtil.checkMap(params);
        PageUtils page = fangkeService.queryPage(params);

        //字典表数据转换
        List<FangkeView> list =(List<FangkeView>)page.getList();
        for(FangkeView c:list){
            //修改对应字典表字段
            dictionaryService.dictionaryConvert(c, request);
        }
        return R.ok().put("data", page);
    }

    /**
    * 后端详情
    */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id, HttpServletRequest request){
        logger.debug("info方法:,,Controller:{},,id:{}",this.getClass().getName(),id);
        FangkeEntity fangke = fangkeService.selectById(id);
        if(fangke !=null){
            //entity转view
            FangkeView view = new FangkeView();
            BeanUtils.copyProperties( fangke , view );//把实体数据重构到view中
            //级联表 老人
            //级联表
            YonghuEntity yonghu = yonghuService.selectById(fangke.getYonghuId());
            if(yonghu != null){
            BeanUtils.copyProperties( yonghu , view ,new String[]{ "id", "createTime", "insertTime", "updateTime", "yonghuId"});//把级联的数据添加到view中,并排除id和创建时间字段,当前表的级联注册表
            view.setYonghuId(yonghu.getId());
            }
            //修改对应字典表字段
            dictionaryService.dictionaryConvert(view, request);
            return R.ok().put("data", view);
        }else {
            return R.error(511,"查不到数据");
        }

    }

    /**
    * 后端保存
    */
    @RequestMapping("/save")
    public R save(@RequestBody FangkeEntity fangke, HttpServletRequest request){
        logger.debug("save方法:,,Controller:{},,fangke:{}",this.getClass().getName(),fangke.toString());

        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(false)
            return R.error(511,"永远不会进入");
        else if("老人".equals(role))
            fangke.setYonghuId(Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId"))));

        Wrapper<FangkeEntity> queryWrapper = new EntityWrapper<FangkeEntity>()
            .eq("fangke_name", fangke.getFangkeName())
            .eq("fangke_mingc", fangke.getFangkeMingc())
            .eq("fangke_phone", fangke.getFangkePhone())
            .eq("fangke_types", fangke.getFangkeTypes())
            .eq("yonghu_id", fangke.getYonghuId())
            .eq("fangke_delete", fangke.getFangkeDelete())
            ;

        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        FangkeEntity fangkeEntity = fangkeService.selectOne(queryWrapper);
        if(fangkeEntity==null){
            fangke.setFangkeDelete(1);
            fangke.setCreateTime(new Date());
            fangkeService.insert(fangke);
            return R.ok();
        }else {
            return R.error(511,"表中有相同数据");
        }
    }

    /**
    * 后端修改
    */
    @RequestMapping("/update")
    public R update(@RequestBody FangkeEntity fangke, HttpServletRequest request) throws NoSuchFieldException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        logger.debug("update方法:,,Controller:{},,fangke:{}",this.getClass().getName(),fangke.toString());
        FangkeEntity oldFangkeEntity = fangkeService.selectById(fangke.getId());//查询原先数据

        String role = String.valueOf(request.getSession().getAttribute("role"));
//        if(false)
//            return R.error(511,"永远不会进入");
//        else if("老人".equals(role))
//            fangke.setYonghuId(Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId"))));
        //根据字段查询是否有相同数据
        Wrapper<FangkeEntity> queryWrapper = new EntityWrapper<FangkeEntity>()
            .notIn("id",fangke.getId())
            .andNew()
            .eq("fangke_name", fangke.getFangkeName())
            .eq("fangke_mingc", fangke.getFangkeMingc())
            .eq("fangke_phone", fangke.getFangkePhone())
            .eq("fangke_types", fangke.getFangkeTypes())
            .eq("yonghu_id", fangke.getYonghuId())
            .eq("fangke_delete", fangke.getFangkeDelete())
            ;

        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        FangkeEntity fangkeEntity = fangkeService.selectOne(queryWrapper);
        if(fangkeEntity==null){
            fangkeService.updateById(fangke);//根据id更新
            return R.ok();
        }else {
            return R.error(511,"表中有相同数据");
        }
    }



    /**
    * 删除
    */
    @RequestMapping("/delete")
    public R delete(@RequestBody Integer[] ids, HttpServletRequest request){
        logger.debug("delete:,,Controller:{},,ids:{}",this.getClass().getName(),ids.toString());
        List<FangkeEntity> oldFangkeList =fangkeService.selectBatchIds(Arrays.asList(ids));//要删除的数据
        ArrayList<FangkeEntity> list = new ArrayList<>();
        for(Integer id:ids){
            FangkeEntity fangkeEntity = new FangkeEntity();
            fangkeEntity.setId(id);
            fangkeEntity.setFangkeDelete(2);
            list.add(fangkeEntity);
        }
        if(list != null && list.size() >0){
            fangkeService.updateBatchById(list);
        }

        return R.ok();
    }


    /**
     * 批量上传
     */
    @RequestMapping("/batchInsert")
    public R save( String fileName, HttpServletRequest request){
        logger.debug("batchInsert方法:,,Controller:{},,fileName:{}",this.getClass().getName(),fileName);
        Integer yonghuId = Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId")));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            List<FangkeEntity> fangkeList = new ArrayList<>();//上传的东西
            Map<String, List<String>> seachFields= new HashMap<>();//要查询的字段
            Date date = new Date();
            int lastIndexOf = fileName.lastIndexOf(".");
            if(lastIndexOf == -1){
                return R.error(511,"该文件没有后缀");
            }else{
                String suffix = fileName.substring(lastIndexOf);
                if(!".xls".equals(suffix)){
                    return R.error(511,"只支持后缀为xls的excel文件");
                }else{
                    URL resource = this.getClass().getClassLoader().getResource("static/upload/" + fileName);//获取文件路径
                    File file = new File(resource.getFile());
                    if(!file.exists()){
                        return R.error(511,"找不到上传文件，请联系管理员");
                    }else{
                        List<List<String>> dataList = PoiUtil.poiImport(file.getPath());//读取xls文件
                        dataList.remove(0);//删除第一行，因为第一行是提示
                        for(List<String> data:dataList){
                            //循环
                            FangkeEntity fangkeEntity = new FangkeEntity();
//                            fangkeEntity.setFangkeName(data.get(0));                    //标题 要改的
//                            fangkeEntity.setFangkeMingc(data.get(0));                    //来访人名称 要改的
//                            fangkeEntity.setFangkePhone(data.get(0));                    //来访人联系方式 要改的
//                            fangkeEntity.setFangkeTypes(Integer.valueOf(data.get(0)));   //来访目的 要改的
//                            fangkeEntity.setFangkeTime(sdf.parse(data.get(0)));          //来访时间 要改的
//                            fangkeEntity.setHuiliaTime(sdf.parse(data.get(0)));          //离开时间 要改的
//                            fangkeEntity.setYonghuId(Integer.valueOf(data.get(0)));   //老人 要改的
//                            fangkeEntity.setFangkeText(data.get(0));                    //备注 要改的
//                            fangkeEntity.setFangkeContent("");//详情和图片
//                            fangkeEntity.setFangkeDelete(1);//逻辑删除字段
//                            fangkeEntity.setCreateTime(date);//时间
                            fangkeList.add(fangkeEntity);


                            //把要查询是否重复的字段放入map中
                                //来访人联系方式
                                if(seachFields.containsKey("fangkePhone")){
                                    List<String> fangkePhone = seachFields.get("fangkePhone");
                                    fangkePhone.add(data.get(0));//要改的
                                }else{
                                    List<String> fangkePhone = new ArrayList<>();
                                    fangkePhone.add(data.get(0));//要改的
                                    seachFields.put("fangkePhone",fangkePhone);
                                }
                        }

                        //查询是否重复
                         //来访人联系方式
                        List<FangkeEntity> fangkeEntities_fangkePhone = fangkeService.selectList(new EntityWrapper<FangkeEntity>().in("fangke_phone", seachFields.get("fangkePhone")).eq("fangke_delete", 1));
                        if(fangkeEntities_fangkePhone.size() >0 ){
                            ArrayList<String> repeatFields = new ArrayList<>();
                            for(FangkeEntity s:fangkeEntities_fangkePhone){
                                repeatFields.add(s.getFangkePhone());
                            }
                            return R.error(511,"数据库的该表中的 [来访人联系方式] 字段已经存在 存在数据为:"+repeatFields.toString());
                        }
                        fangkeService.insertBatch(fangkeList);
                        return R.ok();
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            return R.error(511,"批量插入数据异常，请联系管理员");
        }
    }





}
