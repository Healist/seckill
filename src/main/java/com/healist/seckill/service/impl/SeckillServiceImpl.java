package com.healist.seckill.service.impl;

import com.healist.seckill.dao.SeckillDao;
import com.healist.seckill.dao.SuccessKilledDao;
import com.healist.seckill.dao.cache.RedisDao;
import com.healist.seckill.dto.Exposer;
import com.healist.seckill.dto.SeckillExecution;
import com.healist.seckill.entity.Seckill;
import com.healist.seckill.entity.SuccessKilled;
import com.healist.seckill.enums.SeckillStateEnum;
import com.healist.seckill.exception.RepeteKillException;
import com.healist.seckill.exception.SeckillCloseException;
import com.healist.seckill.exception.SeckillException;
import com.healist.seckill.service.SeckillService;
import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Healist on 2016/11/16.
 */
@Service
public class SeckillServiceImpl implements SeckillService {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    //注入Service依赖
    @Autowired
    private SeckillDao seckillDao;

    @Autowired
    private SuccessKilledDao successKilledDao;
//
    @Autowired
    private RedisDao redisDao;

    //MD5盐值，用于混淆MD5
    private final String slat = "KFJ^&DFDfj(DF6DF5dfo3432*65df";

    public List<Seckill> getSeckillList() {
        return seckillDao.queryAll(0, 4);
    }

    public Seckill getById(long seckillId) {
        return seckillDao.queryById(seckillId);
    }

    public Exposer exportSeckillUrl(long seckillId) {
        //优化点：缓存优化
        // FIXME: 2016/9/21
        //超时的基础上去维护一致性（注：假定相应时间不会变化）

        /**
         * 应该放DAO里面
         * get from cache
         * if null
         *  get db
         * else
         *  put cache
         * logic
         */
        //优化前
//        Seckill seckill = seckillDao.queryById(seckillId);
//        if (seckill == null) {
//            return new Exposer(false, seckillId);
//        }

        //优化后
        Seckill seckill = redisDao.getSeckill(seckillId);
        if (seckill == null) {
            //访问数据库
            seckill = seckillDao.queryById(seckillId);
            if (seckill == null) {
                return new Exposer(false, seckillId);
            } else {
                //放入redis
                redisDao.putSeckill(seckill);
            }
        }

        Date startTime = seckill.getStartTime();
        Date endTime = seckill.getEndTime();
        //当前系统时间
        Date nowTime = new Date();
        if (nowTime.getTime() < startTime.getTime()
                || nowTime.getTime() > endTime.getTime()) {
            return new Exposer(false, seckillId, nowTime.getTime(), startTime.getTime(), endTime.getTime());
        }
        //转换特定字符串的过程，不可逆
        String md5 = getMD5(seckillId);
        return new Exposer(true, md5, seckillId);
    }


    @Transactional
    public SeckillExecution excuteSeckill(long seckillId, long userPhone, String md5)
            throws SeckillException, RepeteKillException, SeckillCloseException {
        if (md5 == null || (!md5.equals(getMD5(seckillId)))) {
            throw new SeckillException("Seckill data rewrite");
        }
        //执行秒杀逻辑：减库存，记录购买行为
        Date nowTime = new Date();
        try {
            //优化前
//            //减库存
//            int updateCount = seckillDao.reduceNumber(seckillId, nowTime);
//            if (updateCount <= 0) {
//                //没有更新到记录，秒杀结束
//                throw new SeckillCloseException("Seckill is closed");
//            } else {
//                //记录购买行为
//                int insertCount = successKilledDao.insertSuccessKilled(seckillId, userPhone);
//                if (insertCount <= 0) {
//                    //重复秒杀
//                    throw new RepeteKillException("Seckill repeated");
//                } else {
//                    //秒杀成功
//                    SuccessKilled successKilled = successKilledDao.queryByIdWithSeckill(seckillId, userPhone);
//                    return new SeckillExecution(seckillId, SeckillStateEnum.SUCCESS, successKilled);
//                }
//            }
            //优化后
            //记录购买行为
            int insertCount = successKilledDao.insertSuccessKilled(seckillId, userPhone);
            if (insertCount <= 0) {
                //重复秒杀
                throw new RepeteKillException("Seckill repeated");
            } else {
                //减库存
                int updateCount = seckillDao.reduceNumber(seckillId, nowTime);
                if (updateCount <= 0) {
                    //没有更新到记录，秒杀结束
                    throw new SeckillCloseException("Seckill is closed");
                } else {
                    //秒杀成功
                    SuccessKilled successKilled = successKilledDao.queryByIdWithSeckill(seckillId, userPhone);
                    return new SeckillExecution(seckillId, SeckillStateEnum.SUCCESS, successKilled);
                }
            }
        } catch (SeckillCloseException e1) {
            throw e1;
        } catch (RepeteKillException e2) {
            throw e2;
        } catch (Exception e) {
            logger.error(e.getMessage());
            //所有编译期异常转换为运行时异常
            throw new SeckillException("Seckill inner error" + e.getMessage());
        }

    }

    public SeckillExecution excuteSeckillProcedure(long seckillId, long userPhone, String md5) {
        if (md5 == null || (!md5.equals(getMD5(seckillId)))) {
            throw new SeckillException("Seckill data rewrite");
        }
        Date killTime = new Date();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("seckillId", seckillId);
        map.put("phone", userPhone);
        map.put("killTime", killTime);
        map.put("result", null);
        //执行存储过程，result被赋值
        try {
            seckillDao.killByProcedure(map);
            //获取result
            int result = MapUtils.getInteger(map, "result", -2);
            if (result == 1) {
                SuccessKilled successKilled = successKilledDao.queryByIdWithSeckill(seckillId, userPhone);
                return new SeckillExecution(seckillId, SeckillStateEnum.SUCCESS, successKilled);
            } else {
                return new SeckillExecution(seckillId, SeckillStateEnum.stateOf(result));
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return new SeckillExecution(seckillId, SeckillStateEnum.INNER_ERROR);
        }
    }

    private String getMD5(long seckillId) {
        String base = seckillId + "/" + slat;
        String md5 = DigestUtils.md5DigestAsHex(base.getBytes());
        return md5;
    }
}
