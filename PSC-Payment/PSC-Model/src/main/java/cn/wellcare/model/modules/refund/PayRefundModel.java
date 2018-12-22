package cn.wellcare.model.modules.refund;

import cn.wellcare.core.utils.CommonUtils;
import cn.wellcare.core.utils.PagerInfo;
import cn.wellcare.dao.refund.PayRefundDao;
import cn.wellcare.entity.refund.PayRefund;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PayRefundModel {

	private Logger log = Logger.getLogger(this.getClass());
    
    @Resource
    private PayRefundDao payRefundDao;
    
    /**
     * 根据id取得退款
     * @param  payRefundId
     * @return
     */
    public PayRefund getPayRefundById(Integer payRefundId) {
    	return payRefundDao.get(payRefundId);
    }
    /**
     * 根据订单id取得退款
     * @param
     * @return
     */
    public PayRefund queryPayRefundById(Integer orderId) {
    	return payRefundDao.queryPayRefund(orderId);
    }

    /**
     * 保存退款
     * @param  payRefund
     * @return
     */
     public Integer savePayRefund(PayRefund payRefund) {
     	this.dbConstrains(payRefund);
     	Date nowTime = new Date();
        payRefund.setCreateTime(nowTime);
     	return payRefundDao.save(payRefund);
     }
     
     /**
     * 更新退款
     * @param  payRefund
     * @return
     */
     public Integer updatePayRefund(PayRefund payRefund) {
     	this.dbConstrains(payRefund);
     	Date nowTime = new Date();
        payRefund.setUpdateTime(nowTime);
     	return payRefundDao.update(payRefund);
     }
     
     private void dbConstrains(PayRefund payRefund) {
		payRefund.setTradeSn(CommonUtils.dbSafeString(payRefund.getTradeSn(), true, 64));
     }
     
    public List<PayRefund> page(Map<String, Object> queryMap, PagerInfo pager) throws Exception {
     	Map<String, Object> param = new HashMap<String, Object>(queryMap);
            Integer start = 0, size = 0;
            if (pager != null) {
                pager.setRowsCount(payRefundDao.getCount(param));
                start = pager.getStart();
                size = pager.getPageSize();
            }
 			param.put("start", start);
            param.put("size", size);
            List<PayRefund> list = payRefundDao.getList(param);
        return list;
    }
     
      public Integer del(Integer id) {
        return payRefundDao.del(id);
     }
     
     
}