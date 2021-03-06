package cn.wellcare.handler.transaction.payment.alipay;

import java.util.Map;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.demo.trade.model.builder.AlipayTradePrecreateRequestBuilder;
import com.alipay.demo.trade.model.result.AlipayF2FPrecreateResult;
import com.alipay.demo.trade.service.AlipayTradeService;
import com.alipay.demo.trade.utils.ZxingUtils;

import cn.wellcare.core.bean.DomainUrlUtil;
import cn.wellcare.core.constant.BaseParam;
import cn.wellcare.core.constant.Constants;
import cn.wellcare.core.exception.BusinessException;
import cn.wellcare.entity.order.PayOrder;
import cn.wellcare.pojo.alipay.AlipayPaymentResult;
import cn.wellcare.pojo.common.PaymentResult;
import cn.wellcare.service.transaction.payment.alipay.base.AlipayPayment;
import cn.wellcare.service.transaction.payment.alipay.base.AlipayTradeBuilder;

/**
 * 支付宝支付
 * 
 * @author zhaihl
 * @date 2018年10月15日
 */
@Service
public class AlipayNativePaymentHandler extends AlipayPayment {
	// 支付宝当面付2.0服务
	private AlipayTradeService tradeService;
	@Resource
	private AlipayTradeBuilder tradeBuilder;
	protected Logger log = Logger.getLogger(this.getClass());


	public PaymentResult doPay(Map<String, Object> param) {
		PaymentResult pr = new PaymentResult();
		try {
			// 1.订单处理
			PayOrder po = (PayOrder) param.get(Constants.ORDERS_INFO);

			String orderPaySn = po.getPaySn();

			// Configs会读取classpath下的zfbinfo.properties文件配置信息，如果找不到该文件则确认该文件是否在classpath目录

			tradeBuilder.setBusid(Integer.valueOf((String) param.get(BaseParam.ORG_ID)));
			tradeService = tradeBuilder.build();
			// (必填) 商户网站订单系统中唯一订单号，64个字符以内，只能包含字母、数字、下划线，
			// 需保证商户系统端不能重复，建议通过数据库sequence生成，

			// (必填) 订单标题，粗略描述用户的支付目的。如“xxx品牌xxx门店当面付扫码消费”
			String subject = Constants.ALIPAY_ORDER_SUBJECT;

			// (必填) 订单总金额，单位为元，不能超过1亿元
			// 如果同时传入了【打折金额】,【不可打折金额】,【订单总金额】三者,则必须满足如下条件:【订单总金额】=【打折金额】+【不可打折金额】
			String totalAmount = (String) param.get(BaseParam.PAY_AMOUNT);

			// (可选) 订单不可打折金额，可以配合商家平台配置折扣活动，如果酒水不参与打折，则将对应金额填写至此字段
			// 如果该值未传入,但传入了【订单总金额】,【打折金额】,则该值默认为【订单总金额】-【打折金额】
			String undiscountableAmount = "0";

			// 卖家支付宝账号ID，用于支持一个签约账号下支持打款到不同的收款账号，(打款到sellerId对应的支付宝账号)
			// 如果该字段为空，则默认为与支付宝签约的商户的PID，也就是appid对应的PID
			String sellerId = "";

			// 订单描述，可以对交易或商品进行一个详细地描述，比如填写"购买商品2件共15.00元"
			String body = Constants.ALIPAY_ORDER_SUBJECT;

			// 商户操作员编号，添加此参数可以为商户操作员做销售统计
			String operatorId = "test_operator_id";

			// (必填) 商户门店编号，通过门店号和商家后台可以配置精准到门店的折扣信息，详询支付宝技术支持
			String storeId = "test_store_id";

			// 业务扩展参数，目前可添加由支付宝分配的系统商编号(通过setSysServiceProviderId方法)，详情请咨询支付宝技术支持
			// ExtendParams extendParams = new ExtendParams();
			// extendParams.setSysServiceProviderId("2088100200300400500");

			// 支付超时，定义为120分钟
			String timeoutExpress = "120m";

			// 商品明细列表，需填写购买商品详细信息，
			// List<GoodsDetail> goodsDetailList = new ArrayList<GoodsDetail>();
			// 创建一个商品信息，参数含义分别为商品id（使用国标）、名称、单价（单位为分）、数量，如果需要添加商品类别，详见GoodsDetail
			// GoodsDetail goods1 = GoodsDetail.newInstance("goods_id001", "xxx小面包", 1000,
			// 1);
			// // 创建好一个商品后添加至商品明细列表
			// goodsDetailList.add(goods1);
			//
			// // 继续创建并添加第一条商品信息，用户购买的产品为“黑人牙刷”，单价为5.00元，购买了两件
			// GoodsDetail goods2 = GoodsDetail.newInstance("goods_id002", "xxx牙刷", 500, 2);
			// goodsDetailList.add(goods2);

			// 创建扫码支付请求builder，设置请求参数
			AlipayTradePrecreateRequestBuilder builder = new AlipayTradePrecreateRequestBuilder().setSubject(subject)
					.setTotalAmount(totalAmount).setOutTradeNo(orderPaySn).setUndiscountableAmount(undiscountableAmount)
					.setSellerId(sellerId).setBody(body).setOperatorId(operatorId).setStoreId(storeId)
					.setTimeoutExpress(timeoutExpress)
					.setNotifyUrl(DomainUrlUtil.PSC_PAYMENT_URL + "/unifyPay/alipayNotify");
			// .setGoodsDetailList(goodsDetailList);

			AlipayF2FPrecreateResult result = tradeService.tradePrecreate(builder);
			AlipayTradePrecreateResponse response = result.getResponse();

			switch (result.getTradeStatus()) {
			case SUCCESS:
				this.log.info("支付宝预下单成功");
				// dumpResponse(response);

				// TODO 正式环境去掉
				String filePath = String.format(System.getProperty("user.home").replace("\\\\", "/") + "/qr-%s.png",
						response.getOutTradeNo());
				this.log.info("filePath:" + filePath);
				ZxingUtils.getQRCodeImge(response.getQrCode(), 256, filePath);
				break;
			case FAILED:
				throw new BusinessException("支付宝预下单失败");
			case UNKNOWN:
				throw new BusinessException("系统异常，预下单状态未知");
			default:
				throw new BusinessException("不支持的交易状态，交易返回异常");
			}
			pr = new AlipayPaymentResult(totalAmount, po.getId(), response.getQrCode());
			pr.setTotalFee(po.getMoneyOrder().toString());
		} catch (Exception e) {
			pr.setSuccess(false);
			throw e;
		}
		return pr;
	}


}
