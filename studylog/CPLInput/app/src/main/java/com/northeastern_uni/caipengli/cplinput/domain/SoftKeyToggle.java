package com.northeastern_uni.caipengli.cplinput.domain;

import android.graphics.drawable.Drawable;

/**
 * Created by caipengli on 16年2月25日.
 * 可转换状态的softkey
 */
public class SoftKeyToggle extends SoftKey {
    /**
     * The current state number is stored in the lowest 8 bits of mKeyMask, this
     * mask is used to get the state number. If the current state is 0, the
     * normal state is enabled; if the current state is more than 0, a toggle
     * state in the toggle state chain will be enabled.
     */
    private static final int KEYMASK_TOGGLE_STATE = 0x000000ff;
    private ToggleState mToggleState;

    public int getToggleStateId() {
        return (mKeyMask & KEYMASK_TOGGLE_STATE);
    }

    /**
     * 是否可以切换到制定的状态s
     * @param stateId
     * @param resetIfNotFound
     * @return
     */
    public boolean enableToggleState(int stateId, boolean resetIfNotFound) {
        int oldStateId = (mKeyMask & KEYMASK_TOGGLE_STATE);
        if (oldStateId == stateId) return false;
        mKeyMask &= (~KEYMASK_TOGGLE_STATE);
        if (stateId > 0) {
            mKeyMask |= (KEYMASK_TOGGLE_STATE & stateId);
            if (getToggleState() == null) {
                mKeyMask &= (~KEYMASK_TOGGLE_STATE);
                if (!resetIfNotFound && oldStateId > 0) {
                    mKeyMask |= (KEYMASK_TOGGLE_STATE & oldStateId);
                }
                return resetIfNotFound;
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

    /**
     * 判断当前状态是否与stateId要求一样,如果是就重置他
     * @param stateId
     * @param resetIfNotFound 列表里没这个状态的花是否重置 0
     * @return
     */
    public boolean disableToggleState(int stateId, boolean resetIfNotFound) {
        int oldStateId = (mKeyMask & KEYMASK_TOGGLE_STATE);
        if (oldStateId == stateId) {
            mKeyMask &= (~KEYMASK_TOGGLE_STATE);
            return stateId != 0;
        }

        if (resetIfNotFound) {
            mKeyMask &= (~KEYMASK_TOGGLE_STATE);
            return oldStateId != 0;
        }
        return false;
    }

    /**
     * 判断当前的state是否和mKeyMask的一致,如果不一致就循环下一个状态至最后
     * @return
     */
    private ToggleState getToggleState() {
        int stateId = (mKeyMask & KEYMASK_TOGGLE_STATE);
        if (0 == stateId) return null;

        ToggleState state = mToggleState;
        while ((null != state)
                && (state.mIdAndFlags & KEYMASK_TOGGLE_STATE) != stateId) {
            state = state.mNextState;
        }
        return state;
    }

    public boolean disableAllToggleStates() {
        int oldStateId = (mKeyMask & KEYMASK_TOGGLE_STATE);
        mKeyMask &= (~KEYMASK_TOGGLE_STATE);
        return oldStateId != 0;
    }

    @Override
    public Drawable getKeyIcon() {
        ToggleState state = getToggleState();
        if (null != state) return state.mKeyIcon;
        return super.getKeyIcon();
    }

    @Override
    public Drawable getKeyIconPopup() {
        ToggleState state = getToggleState();
        if (null != state) {
            if (null != state.mKeyIconPopup) {
                return state.mKeyIconPopup;
            } else {
                return state.mKeyIcon;
            }
        }
        return super.getKeyIconPopup();
    }

    @Override
    public int getKeyCode() {
        ToggleState state = getToggleState();
        if (null != state) return state.mKeyCode;
        return mKeyCode;
    }

    @Override
    public String getKeyLabel() {
        ToggleState state = getToggleState();
        if (null != state) return state.mKeyLabel;
        return mKeyLabel;
    }

    @Override
    public Drawable getKeyBg() {
        ToggleState state = getToggleState();
        if (null != state && null != state.mKeyType) {
            return state.mKeyType.mKeyBg;
        }
        return mKeyType.mKeyBg;
    }

    @Override
    public int getColor() {
        ToggleState state = getToggleState();
        if (null != state && null != state.mKeyType) {
            return state.mKeyType.mColor;
        }
        return mKeyType.mColor;
    }

    @Override
    public int getColorHl() {
        ToggleState state = getToggleState();
        if (null != state && null != state.mKeyType) {
            return state.mKeyType.mColorHl;
        }
        return mKeyType.mColorHl;
    }
    @Override
    public Drawable getKeyHlBg() {
        ToggleState state = getToggleState();
        if (null != state && null != state.mKeyType) {
            return state.mKeyType.mKeyHlBg;
        }
        return mKeyType.mKeyHlBg;
    }

    @Override
    public int getColorBalloon() {
        ToggleState state = getToggleState();
        if (null != state && null != state.mKeyType) {
            return state.mKeyType.mColorBalloon;
        }
        return mKeyType.mColorBalloon;
    }

    @Override
    public boolean isKeyCodeKey() {
        ToggleState state = getToggleState();
        if (null != state) {
            if (state.mKeyCode > 0) return true;
            return false;
        }
        return super.isKeyCodeKey();
    }

    @Override
    public boolean isUserDefKey() {
        ToggleState state = getToggleState();
        if (null != state) {
            if (state.mKeyCode < 0) return true;
            return false;
        }
        return super.isUserDefKey();
    }

    @Override
    public boolean isUniStrKey() {
        ToggleState state = getToggleState();
        if (null != state) {
            if (null != state.mKeyLabel && state.mKeyCode == 0) {
                return true;
            }
            return false;
        }
        return super.isUniStrKey();
    }

    @Override
    public boolean needBalloon() {
        ToggleState state = getToggleState();
        if (null != state) {
            return (state.mIdAndFlags & KEYMASK_BALLOON) != 0;
        }
        return super.needBalloon();
    }

    @Override
    public boolean repeatable() {
        ToggleState state = getToggleState();
        if (null != state) {
            return (state.mIdAndFlags & KEYMASK_REPEAT) != 0;
        }
        return super.repeatable();
    }

    @Override
    public void changeCase(boolean lowerCase) {
        ToggleState state = getToggleState();
        if (null != state && null != state.mKeyLabel) {
            if (lowerCase)
                state.mKeyLabel = state.mKeyLabel.toLowerCase();
            else
                state.mKeyLabel = state.mKeyLabel.toUpperCase();
        }
    }

    public ToggleState createToggleState() {
        return new ToggleState();
    }

    public boolean setToggleStates(ToggleState rootState) {
        if (null == rootState) return false;
        mToggleState = rootState;
        return true;
    }

    /**
     * 就相当于一个state的列表里面有个各个state
     */
    public class ToggleState {
        private int mIdAndFlags;//id必须大于0
        public SoftKeyType mKeyType;
        public int mKeyCode;
        public Drawable mKeyIcon;
        public Drawable mKeyIconPopup;
        public String mKeyLabel;
        public ToggleState mNextState;

        public void setStateId(int stateId) {
            mIdAndFlags |= (stateId & KEYMASK_TOGGLE_STATE);
        }

        public void setStateFlags(boolean repeat, boolean balloon) {
            if (repeat) {
                mIdAndFlags |= KEYMASK_REPEAT;
            } else {
                mIdAndFlags &= (~KEYMASK_REPEAT);
            }

            if (balloon) {
                mIdAndFlags |= KEYMASK_BALLOON;
            } else {
                mIdAndFlags &= (~KEYMASK_BALLOON);
            }
        }
    }
}
