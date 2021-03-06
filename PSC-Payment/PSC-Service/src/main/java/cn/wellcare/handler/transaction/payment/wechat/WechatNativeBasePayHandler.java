package cn.wellcare.handler.transaction.payment.wechat;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import cn.wellcare.core.constant.BaseParam;
import cn.wellcare.core.constant.Constants;
import cn.wellcare.core.exception.BusinessException;
import cn.wellcare.core.utils.CommonUtils;
import cn.wellcare.entity.order.PayOrder;
import cn.wellcare.pojo.common.PaymentResult;
import cn.wellcare.pojo.wechat.WechatPaymentResult;
import cn.wellcare.service.transaction.payment.wechat.base.WechatPayment;
import cn.wellcare.service.transaction.payment.wechat.util.GetWxOrderno;

/**
 * 微信扫码支付(主动)
 * 
 * @author zhaihl
 *
 */
@Service
public class WechatNativeBasePayHandler extends WechatPayment {

	protected Logger log = Logger.getLogger(this.getClass());

	public PaymentResult doPay(Map<String, Object> param) {
		PaymentResult pr = new PaymentResult();
		try {
			// 1.订单处理
			PayOrder order = (PayOrder) param.get(Constants.ORDERS_INFO);
			Assert.notNull(order);

			String orderPaySn = order.getPaySn();

			// 2.支付
			String totalFee = (String) param.get(BaseParam.PAY_AMOUNT);

			// 支付的总金额（分）
			BigDecimal needsPay = new BigDecimal(totalFee);
			needsPay = needsPay.multiply(new BigDecimal(100));
			String txnAmt = needsPay.toString().split("\\.")[0]; // 付款金额，单位为分，不能有小数点，去掉

			// 32位随机字符串
			String nonce_str = CommonUtils.getRandomString(32);
			// 获取本机Ip
			String ip = CommonUtils.localIp();

			// 建立时间格式化对象：
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
			// 获取当前时间，作为订单开始时间获取到的时间类型是long类型的，单位是毫秒
			long currentTime = System.currentTimeMillis();
			String currentTimeStr = dateFormat.format(new Date(currentTime));
			// 在这个基础上加上5分钟：作为订单失效时间
			long currentTimeAddFive = currentTime + 5 * 60 * 1000;
			String currentTimeAddFiveStr = dateFormat.format(new Date(currentTimeAddFive));

			// 加密串
			SortedMap<String, String> packageParams = new TreeMap<String, String>();

			initConfig(Integer.valueOf((String) param.get(BaseParam.ORG_ID)));
			packageParams.put("appid", appid);
			packageParams.put("mch_id", mchid);
			packageParams.put("product_id", orderPaySn); // 商户根据自己业务传递的参数 必填
			packageParams.put("nonce_str", nonce_str);
			packageParams.put("body", Constants.WEIXIN_ORDER_NAME);
			packageParams.put("out_trade_no", orderPaySn);
			packageParams.put("total_fee", txnAmt); // 支付金额,精确到分 txnAmt
			packageParams.put("spbill_create_ip", ip);
			packageParams.put("notify_url", notifyURL);
			packageParams.put("trade_type", "NATIVE");
			packageParams.put("time_start", currentTimeStr);
			packageParams.put("time_expire", currentTimeAddFiveStr);
			String sign = createSign(packageParams, apikey);
			packageParams.put("sign", sign);

			// xml传输数据
			String xml = "<xml>" + "<appid>" + appid + "</appid>" + "<mch_id>" + mchid + "</mch_id>" + "<product_id>"
					+ orderPaySn + "</product_id>" + "<nonce_str>" + nonce_str + "</nonce_str>" + "<body>"
					+ Constants.WEIXIN_ORDER_NAME + "</body>" + "<out_trade_no>" + orderPaySn + "</out_trade_no>"
					+ "<total_fee>" + txnAmt + "</total_fee>" + "<spbill_create_ip>" + ip + "</spbill_create_ip>"
					+ "<notify_url>" + notifyURL + "</notify_url>" + "<trade_type>NATIVE</trade_type>" + "<time_start>"
					+ currentTimeStr + "</time_start>" + "<time_expire>" + currentTimeAddFiveStr + "</time_expire>"
					+ "<sign>" + sign + "</sign>" + "</xml>";

			Map<String, Object> map = GetWxOrderno.getPayNo(createOrderURL, xml);

			if (null == map) {
				throw new BusinessException("支付接口异常，请稍后重试");
			}

			String returnCode = (String) map.get("return_code");

			if (!"SUCCESS".equals(returnCode)) {
				throw new BusinessException("支付接口异常，请稍后重试");
			}

			String resultCode = (String) map.get("result_code");
			if (!"SUCCESS".equals(resultCode)) {
				throw new BusinessException("支付接口异常，请稍后重试");
			}

			// 获取生成二维码的参数
			String codeUrl = (String) map.get("code_url");
			if (CommonUtils.isNull(codeUrl)) {
				throw new BusinessException("支付接口异常，请稍后重试");
			}

			pr = new WechatPaymentResult(totalFee, order.getId(), codeUrl);
		} catch (Exception e) {
			pr.setSuccess(false);
			throw e;
		}
		return pr;
	}

}
