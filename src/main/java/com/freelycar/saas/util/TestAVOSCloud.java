package com.freelycar.saas.util;


import cn.leancloud.core.AVOSCloud;
import cn.leancloud.sms.AVSMS;
import cn.leancloud.sms.AVSMSOption;
import cn.leancloud.types.AVNull;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

/**
 * LeanCloud 短信认证 Java SDK 调用方式 demo
 * （2019-09会推出新SDK，暂时项目中还是采用URL调用方式，此demo不删除，作为参考）
 *
 * @author tangwei - Toby
 * @date 2019-07-12
 * @email toby911115@gmail.com
 */
public class TestAVOSCloud {
    public static void main(String[] args) {
        // 参数依次为 AppId、AppKey、MasterKey

        String phone = "18206295380";
        String code = "659268";
        smsCode("18206295380");
//        verifySMSCode(phone, code);
    }

    public static void smsCode(String phone) {
        AVSMSOption option = new AVSMSOption();
        option.setTtl(1);
        option.setApplicationName("小易爱车");
        option.setOperation("短信认证");
        AVSMS.requestSMSCodeInBackground(phone, option)
                .subscribe(new Observer<AVNull>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(AVNull avNull) {
                        System.out.println("Result: Successfully sent verification code.");
                    }

                    @Override
                    public void onError(Throwable e) {
                        System.out.println("Result: Failed to send verification code. Reason: " + e.getMessage());
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    public static void verifySMSCode(String phone, String code) {
        AVSMS.verifySMSCodeInBackground(code, phone).subscribe(new Observer<AVNull>() {
            @Override
            public void onSubscribe(Disposable d) {
            }

            @Override
            public void onNext(AVNull avNull) {
                System.out.println("Result: Successfully verified the number.");
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("Result: Failed to verify the number. Reason: " + throwable.getMessage());
            }

            @Override
            public void onComplete() {
            }
        });
    }

}
