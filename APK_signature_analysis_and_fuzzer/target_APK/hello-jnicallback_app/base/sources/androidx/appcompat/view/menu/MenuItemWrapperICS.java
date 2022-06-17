package androidx.appcompat.view.menu;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.ActionProvider;
import android.view.CollapsibleActionView;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.FrameLayout;
import androidx.core.internal.view.SupportMenuItem;
import java.lang.reflect.Method;
/* loaded from: classes.dex */
public class MenuItemWrapperICS extends BaseMenuWrapper<SupportMenuItem> implements MenuItem {
    static final String LOG_TAG = "MenuItemWrapper";
    private Method mSetExclusiveCheckableMethod;

    public MenuItemWrapperICS(Context context, SupportMenuItem object) {
        super(context, object);
    }

    @Override // android.view.MenuItem
    public int getItemId() {
        return ((SupportMenuItem) this.mWrappedObject).getItemId();
    }

    @Override // android.view.MenuItem
    public int getGroupId() {
        return ((SupportMenuItem) this.mWrappedObject).getGroupId();
    }

    @Override // android.view.MenuItem
    public int getOrder() {
        return ((SupportMenuItem) this.mWrappedObject).getOrder();
    }

    @Override // android.view.MenuItem
    public MenuItem setTitle(CharSequence title) {
        ((SupportMenuItem) this.mWrappedObject).setTitle(title);
        return this;
    }

    @Override // android.view.MenuItem
    public MenuItem setTitle(int title) {
        ((SupportMenuItem) this.mWrappedObject).setTitle(title);
        return this;
    }

    @Override // android.view.MenuItem
    public CharSequence getTitle() {
        return ((SupportMenuItem) this.mWrappedObject).getTitle();
    }

    @Override // android.view.MenuItem
    public MenuItem setTitleCondensed(CharSequence title) {
        ((SupportMenuItem) this.mWrappedObject).setTitleCondensed(title);
        return this;
    }

    @Override // android.view.MenuItem
    public CharSequence getTitleCondensed() {
        return ((SupportMenuItem) this.mWrappedObject).getTitleCondensed();
    }

    @Override // android.view.MenuItem
    public MenuItem setIcon(Drawable icon) {
        ((SupportMenuItem) this.mWrappedObject).setIcon(icon);
        return this;
    }

    @Override // android.view.MenuItem
    public MenuItem setIcon(int iconRes) {
        ((SupportMenuItem) this.mWrappedObject).setIcon(iconRes);
        return this;
    }

    @Override // android.view.MenuItem
    public Drawable getIcon() {
        return ((SupportMenuItem) this.mWrappedObject).getIcon();
    }

    @Override // android.view.MenuItem
    public MenuItem setIntent(Intent intent) {
        ((SupportMenuItem) this.mWrappedObject).setIntent(intent);
        return this;
    }

    @Override // android.view.MenuItem
    public Intent getIntent() {
        return ((SupportMenuItem) this.mWrappedObject).getIntent();
    }

    @Override // android.view.MenuItem
    public MenuItem setShortcut(char numericChar, char alphaChar) {
        ((SupportMenuItem) this.mWrappedObject).setShortcut(numericChar, alphaChar);
        return this;
    }

    @Override // android.view.MenuItem
    public MenuItem setShortcut(char numericChar, char alphaChar, int numericModifiers, int alphaModifiers) {
        ((SupportMenuItem) this.mWrappedObject).setShortcut(numericChar, alphaChar, numericModifiers, alphaModifiers);
        return this;
    }

    @Override // android.view.MenuItem
    public MenuItem setNumericShortcut(char numericChar) {
        ((SupportMenuItem) this.mWrappedObject).setNumericShortcut(numericChar);
        return this;
    }

    @Override // android.view.MenuItem
    public MenuItem setNumericShortcut(char numericChar, int numericModifiers) {
        ((SupportMenuItem) this.mWrappedObject).setNumericShortcut(numericChar, numericModifiers);
        return this;
    }

    @Override // android.view.MenuItem
    public char getNumericShortcut() {
        return ((SupportMenuItem) this.mWrappedObject).getNumericShortcut();
    }

    @Override // android.view.MenuItem
    public int getNumericModifiers() {
        return ((SupportMenuItem) this.mWrappedObject).getNumericModifiers();
    }

    @Override // android.view.MenuItem
    public MenuItem setAlphabeticShortcut(char alphaChar) {
        ((SupportMenuItem) this.mWrappedObject).setAlphabeticShortcut(alphaChar);
        return this;
    }

    @Override // android.view.MenuItem
    public MenuItem setAlphabeticShortcut(char alphaChar, int alphaModifiers) {
        ((SupportMenuItem) this.mWrappedObject).setAlphabeticShortcut(alphaChar, alphaModifiers);
        return this;
    }

    @Override // android.view.MenuItem
    public char getAlphabeticShortcut() {
        return ((SupportMenuItem) this.mWrappedObject).getAlphabeticShortcut();
    }

    @Override // android.view.MenuItem
    public int getAlphabeticModifiers() {
        return ((SupportMenuItem) this.mWrappedObject).getAlphabeticModifiers();
    }

    @Override // android.view.MenuItem
    public MenuItem setCheckable(boolean checkable) {
        ((SupportMenuItem) this.mWrappedObject).setCheckable(checkable);
        return this;
    }

    @Override // android.view.MenuItem
    public boolean isCheckable() {
        return ((SupportMenuItem) this.mWrappedObject).isCheckable();
    }

    @Override // android.view.MenuItem
    public MenuItem setChecked(boolean checked) {
        ((SupportMenuItem) this.mWrappedObject).setChecked(checked);
        return this;
    }

    @Override // android.view.MenuItem
    public boolean isChecked() {
        return ((SupportMenuItem) this.mWrappedObject).isChecked();
    }

    @Override // android.view.MenuItem
    public MenuItem setVisible(boolean visible) {
        return ((SupportMenuItem) this.mWrappedObject).setVisible(visible);
    }

    @Override // android.view.MenuItem
    public boolean isVisible() {
        return ((SupportMenuItem) this.mWrappedObject).isVisible();
    }

    @Override // android.view.MenuItem
    public MenuItem setEnabled(boolean enabled) {
        ((SupportMenuItem) this.mWrappedObject).setEnabled(enabled);
        return this;
    }

    @Override // android.view.MenuItem
    public boolean isEnabled() {
        return ((SupportMenuItem) this.mWrappedObject).isEnabled();
    }

    @Override // android.view.MenuItem
    public boolean hasSubMenu() {
        return ((SupportMenuItem) this.mWrappedObject).hasSubMenu();
    }

    @Override // android.view.MenuItem
    public SubMenu getSubMenu() {
        return getSubMenuWrapper(((SupportMenuItem) this.mWrappedObject).getSubMenu());
    }

    @Override // android.view.MenuItem
    public MenuItem setOnMenuItemClickListener(MenuItem.OnMenuItemClickListener menuItemClickListener) {
        ((SupportMenuItem) this.mWrappedObject).setOnMenuItemClickListener(menuItemClickListener != null ? new OnMenuItemClickListenerWrapper(menuItemClickListener) : null);
        return this;
    }

    @Override // android.view.MenuItem
    public ContextMenu.ContextMenuInfo getMenuInfo() {
        return ((SupportMenuItem) this.mWrappedObject).getMenuInfo();
    }

    @Override // android.view.MenuItem
    public void setShowAsAction(int actionEnum) {
        ((SupportMenuItem) this.mWrappedObject).setShowAsAction(actionEnum);
    }

    @Override // android.view.MenuItem
    public MenuItem setShowAsActionFlags(int actionEnum) {
        ((SupportMenuItem) this.mWrappedObject).setShowAsActionFlags(actionEnum);
        return this;
    }

    @Override // android.view.MenuItem
    public MenuItem setActionView(View view) {
        if (view instanceof CollapsibleActionView) {
            view = new CollapsibleActionViewWrapper(view);
        }
        ((SupportMenuItem) this.mWrappedObject).setActionView(view);
        return this;
    }

    @Override // android.view.MenuItem
    public MenuItem setActionView(int resId) {
        ((SupportMenuItem) this.mWrappedObject).setActionView(resId);
        View actionView = ((SupportMenuItem) this.mWrappedObject).getActionView();
        if (actionView instanceof CollapsibleActionView) {
            ((SupportMenuItem) this.mWrappedObject).setActionView(new CollapsibleActionViewWrapper(actionView));
        }
        return this;
    }

    @Override // android.view.MenuItem
    public View getActionView() {
        View actionView = ((SupportMenuItem) this.mWrappedObject).getActionView();
        if (actionView instanceof CollapsibleActionViewWrapper) {
            return ((CollapsibleActionViewWrapper) actionView).getWrappedView();
        }
        return actionView;
    }

    @Override // android.view.MenuItem
    public MenuItem setActionProvider(ActionProvider provider) {
        ((SupportMenuItem) this.mWrappedObject).setSupportActionProvider(provider != null ? createActionProviderWrapper(provider) : null);
        return this;
    }

    @Override // android.view.MenuItem
    public ActionProvider getActionProvider() {
        androidx.core.view.ActionProvider provider = ((SupportMenuItem) this.mWrappedObject).getSupportActionProvider();
        if (provider instanceof ActionProviderWrapper) {
            return ((ActionProviderWrapper) provider).mInner;
        }
        return null;
    }

    @Override // android.view.MenuItem
    public boolean expandActionView() {
        return ((SupportMenuItem) this.mWrappedObject).expandActionView();
    }

    @Override // android.view.MenuItem
    public boolean collapseActionView() {
        return ((SupportMenuItem) this.mWrappedObject).collapseActionView();
    }

    @Override // android.view.MenuItem
    public boolean isActionViewExpanded() {
        return ((SupportMenuItem) this.mWrappedObject).isActionViewExpanded();
    }

    @Override // android.view.MenuItem
    public MenuItem setOnActionExpandListener(MenuItem.OnActionExpandListener listener) {
        ((SupportMenuItem) this.mWrappedObject).setOnActionExpandListener(listener != null ? new OnActionExpandListenerWrapper(listener) : null);
        return this;
    }

    @Override // android.view.MenuItem
    public MenuItem setContentDescription(CharSequence contentDescription) {
        ((SupportMenuItem) this.mWrappedObject).setContentDescription(contentDescription);
        return this;
    }

    @Override // android.view.MenuItem
    public CharSequence getContentDescription() {
        return ((SupportMenuItem) this.mWrappedObject).getContentDescription();
    }

    @Override // android.view.MenuItem
    public MenuItem setTooltipText(CharSequence tooltipText) {
        ((SupportMenuItem) this.mWrappedObject).setTooltipText(tooltipText);
        return this;
    }

    @Override // android.view.MenuItem
    public CharSequence getTooltipText() {
        return ((SupportMenuItem) this.mWrappedObject).getTooltipText();
    }

    @Override // android.view.MenuItem
    public MenuItem setIconTintList(ColorStateList tint) {
        ((SupportMenuItem) this.mWrappedObject).setIconTintList(tint);
        return this;
    }

    @Override // android.view.MenuItem
    public ColorStateList getIconTintList() {
        return ((SupportMenuItem) this.mWrappedObject).getIconTintList();
    }

    @Override // android.view.MenuItem
    public MenuItem setIconTintMode(PorterDuff.Mode tintMode) {
        ((SupportMenuItem) this.mWrappedObject).setIconTintMode(tintMode);
        return this;
    }

    @Override // android.view.MenuItem
    public PorterDuff.Mode getIconTintMode() {
        return ((SupportMenuItem) this.mWrappedObject).getIconTintMode();
    }

    public void setExclusiveCheckable(boolean checkable) {
        try {
            if (this.mSetExclusiveCheckableMethod == null) {
                this.mSetExclusiveCheckableMethod = ((SupportMenuItem) this.mWrappedObject).getClass().getDeclaredMethod("setExclusiveCheckable", Boolean.TYPE);
            }
            this.mSetExclusiveCheckableMethod.invoke(this.mWrappedObject, Boolean.valueOf(checkable));
        } catch (Exception e) {
            Log.w(LOG_TAG, "Error while calling setExclusiveCheckable", e);
        }
    }

    ActionProviderWrapper createActionProviderWrapper(ActionProvider provider) {
        return new ActionProviderWrapper(this.mContext, provider);
    }

    /* loaded from: classes.dex */
    private class OnMenuItemClickListenerWrapper extends BaseWrapper<MenuItem.OnMenuItemClickListener> implements MenuItem.OnMenuItemClickListener {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        OnMenuItemClickListenerWrapper(MenuItem.OnMenuItemClickListener object) {
            super(object);
            MenuItemWrapperICS.this = r1;
        }

        @Override // android.view.MenuItem.OnMenuItemClickListener
        public boolean onMenuItemClick(MenuItem item) {
            return ((MenuItem.OnMenuItemClickListener) this.mWrappedObject).onMenuItemClick(MenuItemWrapperICS.this.getMenuItemWrapper(item));
        }
    }

    /* loaded from: classes.dex */
    private class OnActionExpandListenerWrapper extends BaseWrapper<MenuItem.OnActionExpandListener> implements MenuItem.OnActionExpandListener {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        OnActionExpandListenerWrapper(MenuItem.OnActionExpandListener object) {
            super(object);
            MenuItemWrapperICS.this = r1;
        }

        @Override // android.view.MenuItem.OnActionExpandListener
        public boolean onMenuItemActionExpand(MenuItem item) {
            return ((MenuItem.OnActionExpandListener) this.mWrappedObject).onMenuItemActionExpand(MenuItemWrapperICS.this.getMenuItemWrapper(item));
        }

        @Override // android.view.MenuItem.OnActionExpandListener
        public boolean onMenuItemActionCollapse(MenuItem item) {
            return ((MenuItem.OnActionExpandListener) this.mWrappedObject).onMenuItemActionCollapse(MenuItemWrapperICS.this.getMenuItemWrapper(item));
        }
    }

    /* loaded from: classes.dex */
    public class ActionProviderWrapper extends androidx.core.view.ActionProvider {
        final ActionProvider mInner;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public ActionProviderWrapper(Context context, ActionProvider inner) {
            super(context);
            MenuItemWrapperICS.this = this$0;
            this.mInner = inner;
        }

        @Override // androidx.core.view.ActionProvider
        public View onCreateActionView() {
            return this.mInner.onCreateActionView();
        }

        @Override // androidx.core.view.ActionProvider
        public boolean onPerformDefaultAction() {
            return this.mInner.onPerformDefaultAction();
        }

        @Override // androidx.core.view.ActionProvider
        public boolean hasSubMenu() {
            return this.mInner.hasSubMenu();
        }

        @Override // androidx.core.view.ActionProvider
        public void onPrepareSubMenu(SubMenu subMenu) {
            this.mInner.onPrepareSubMenu(MenuItemWrapperICS.this.getSubMenuWrapper(subMenu));
        }
    }

    /* loaded from: classes.dex */
    static class CollapsibleActionViewWrapper extends FrameLayout implements androidx.appcompat.view.CollapsibleActionView {
        final CollapsibleActionView mWrappedView;

        CollapsibleActionViewWrapper(View actionView) {
            super(actionView.getContext());
            this.mWrappedView = (CollapsibleActionView) actionView;
            addView(actionView);
        }

        @Override // androidx.appcompat.view.CollapsibleActionView
        public void onActionViewExpanded() {
            this.mWrappedView.onActionViewExpanded();
        }

        @Override // androidx.appcompat.view.CollapsibleActionView
        public void onActionViewCollapsed() {
            this.mWrappedView.onActionViewCollapsed();
        }

        View getWrappedView() {
            return (View) this.mWrappedView;
        }
    }
}
