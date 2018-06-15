package cn.jnvc.toolslib;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

/**
 * Created by Administrator on 2018/6/15.
 */
public class LoadingDialog extends Dialog {

    public LoadingDialog(Context context) {
        super(context);
    }

    public LoadingDialog(Context context, int themeResId) {
        super(context, themeResId);
    }

    public static class Builder {

        private Context context;
        private String message;// 布局中的文字
        private boolean isShowMessage = true;// 是否显示文字
        private boolean isCancelable=false;// 按返回键取消
        private boolean isCancelOutside = false;// 是否可以控制取消

        public Builder(Context context){
            this.context = context;
        }

        /**
         *  设置提示信息
         *  @param message
         *  @return
         * */
        public Builder setMessage(String message) {
            this.message = message;
            return this;
        }

        /**
         *  设置是否显示提示信息
         *  @param isShowMessage
         *  @return
         * */
        public Builder setShowMessage(boolean isShowMessage) {
            this.isShowMessage = isShowMessage;
            return this;
        }

        /**
         *  设置是否可以按返回键取消
         *  @param isCancelable
         *  @return
         * */
        public Builder setCancelable(boolean isCancelable){
            this.isCancelable=isCancelable;
            return this;
        }

        /**
         *  设置是否可以取消
         *  @param isCancelOutside
         *  @return
         * */
        public Builder setCancelOutside(boolean isCancelOutside){
            this.isCancelOutside=isCancelOutside;
            return this;
        }

        public LoadingDialog create() {
            /*
             *  获取并设置loading的布局文件
             * */
            LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(R.layout.dialog_loading,null);
            /*
             *  设置Dialog的布局样式
             * */
            LoadingDialog LoadingDialog=new LoadingDialog(context,R.style.MyDialogStyle);
            /*
             *  初始化dialog_loading中的控件
             * */
            TextView HintText= (TextView) view.findViewById(R.id.dialog_loading_tv_HintText);//loading显示提示文字

            if(isShowMessage){
                HintText.setText(message);
            }else{
                HintText.setVisibility(View.GONE);
            }

            LoadingDialog.setContentView(view);
            LoadingDialog.setCancelable(isCancelable);
            LoadingDialog.setCanceledOnTouchOutside(isCancelOutside);

            return  LoadingDialog;
        }

    }
}
