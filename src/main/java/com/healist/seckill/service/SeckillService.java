package com.healist.seckill.service;

import com.healist.seckill.dto.Exposer;
import com.healist.seckill.dto.SeckillExecution;
import com.healist.seckill.entity.Seckill;
import com.healist.seckill.exception.RepeteKillException;
import com.healist.seckill.exception.SeckillCloseException;
import com.healist.seckill.exception.SeckillException;

import java.util.List;

/**
 * Created by Healist on 2016/11/16.
 */
public interface SeckillService {

    /**
     * 查询所有秒杀记录
     * @return
     */
    List<Seckill> getSeckillList();


    /**
     * 查询单个秒杀记录
     * @param seckillId
     * @return
     */
    Seckill getById(long seckillId);


    /**
     * 秒杀开启时，输出秒杀接口地址
     * 否则输出系统时间和秒杀时间
     * @param seckillId
     */
    Exposer exportSeckillUrl(long seckillId);


    /**
     * 执行秒杀操作
     * @param seckillId
     * @param userPhone
     * @param md5
     */
    SeckillExecution excuteSeckill(long seckillId, long userPhone, String md5)
            throws SeckillException,RepeteKillException,SeckillCloseException;

    /**
     * 执行秒杀操作
     * @param seckillId
     * @param userPhone
     * @param md5
     */
    SeckillExecution excuteSeckillProcedure(long seckillId, long userPhone, String md5);

}
