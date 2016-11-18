package com.healist.seckill.service;

import com.healist.seckill.dto.Exposer;
import com.healist.seckill.dto.SeckillExecution;
import com.healist.seckill.entity.Seckill;
import com.healist.seckill.exception.RepeteKillException;
import com.healist.seckill.exception.SeckillCloseException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by Healist on 2016/11/16.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({
        "classpath:spring/spring-service.xml",
        "classpath:spring/spring-dao.xml"
})
public class SeckillServiceTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private SeckillService seckillService;

    @Test
    public void getSeckillList() throws Exception {
        List<Seckill> list = seckillService.getSeckillList();
        logger.info("list={}", list);
    }

    @Test
    public void getById() throws Exception {
        long id = 1000L;
        Seckill seckill = seckillService.getById(id);
        logger.info("seckill={}",seckill);
    }

//    @Test
//    public void exportSeckillUrl() throws Exception {
//        long id = 1000L;
//        Exposer exposer = seckillService.exportSeckillUrl(id);
//        logger.info("exposer: " + exposer);
//    }
//
//    @Test
//    public void excuteSeckill() throws Exception {
//        long id = 1000L;
//        long phoneNumber = 13574235323L;
//        String md5 = "79a2e0d328a4a2ac9b8a96cdc6674787";
//
//        try {
//            SeckillExecution seckillExecution = seckillService.excuteSeckill(id, phoneNumber, md5);
//            logger.info("seckillExecution:" + seckillExecution);
//        } catch (SeckillCloseException e) {
//            logger.error(e.getMessage());
//        } catch (RepeteKillException e) {
//            logger.error(e.getMessage());
//        }
//    }

    //将以上俩个应该合起来，通过第一个获取md5盐值，继而执行秒杀业务
    @Test
    public void testSeckillLogic() throws Exception {
        long id = 1003L;
        Exposer exposer = seckillService.exportSeckillUrl(id);
        if (exposer.isExposed()) {
            logger.info("exposer: " + exposer);
            long phoneNumber = 13574678323L;
            String md5 = exposer.getMd5();

            try {
                SeckillExecution seckillExecution = seckillService.excuteSeckill(id, phoneNumber, md5);
                logger.info("seckillExecution:" + seckillExecution);
            } catch (SeckillCloseException e) {
                logger.error(e.getMessage());
            } catch (RepeteKillException e) {
                logger.error(e.getMessage());
            }
        } else {
            //秒杀未开启
            logger.info("exposer: " + exposer);
        }
    }

    @Test
    public void excuteSeckillProcedure() throws Exception {
        long seckillId = 1002L;
        long phone = 17343545434L;
        Exposer exposer = seckillService.exportSeckillUrl(seckillId);
        if (exposer.isExposed()) {
            String md5 = exposer.getMd5();
            SeckillExecution seckillExecution = seckillService.excuteSeckillProcedure(seckillId, phone, md5);
            logger.info(seckillExecution.getStateInfo());
        }
    }
}