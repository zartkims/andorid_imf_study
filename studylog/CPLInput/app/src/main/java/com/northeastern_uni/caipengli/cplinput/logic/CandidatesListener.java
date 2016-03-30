package com.northeastern_uni.caipengli.cplinput.logic;

/**
 * Created by caipengli on 16年3月3日.
 */
public interface CandidatesListener {
    void onClickChoice(int choiced);
    void onToLeftGesture();//之后将会换成滑动
    void onToRightGesture();

    /**
     *　切换输入法
     */
    void onSwitchFloatMode();

    /**
     * 隐藏输入窗口
     */
    void hideWindow();

    /**
     * 进入详细设置页面
     */
    void requestSetting();

    /**
     * 展开调整大小popup
     */
    void showResizePopup();
}
