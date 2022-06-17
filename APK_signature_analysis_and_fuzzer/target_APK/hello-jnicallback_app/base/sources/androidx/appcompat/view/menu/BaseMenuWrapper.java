package androidx.appcompat.view.menu;

import android.content.Context;
import android.view.MenuItem;
import android.view.SubMenu;
import androidx.collection.ArrayMap;
import androidx.core.internal.view.SupportMenuItem;
import androidx.core.internal.view.SupportSubMenu;
import java.util.Iterator;
import java.util.Map;
/* loaded from: classes.dex */
abstract class BaseMenuWrapper<T> extends BaseWrapper<T> {
    final Context mContext;
    private Map<SupportMenuItem, MenuItem> mMenuItems;
    private Map<SupportSubMenu, SubMenu> mSubMenus;

    public BaseMenuWrapper(Context context, T object) {
        super(object);
        this.mContext = context;
    }

    public final MenuItem getMenuItemWrapper(MenuItem menuItem) {
        if (menuItem instanceof SupportMenuItem) {
            SupportMenuItem supportMenuItem = (SupportMenuItem) menuItem;
            if (this.mMenuItems == null) {
                this.mMenuItems = new ArrayMap();
            }
            MenuItem wrappedItem = this.mMenuItems.get(menuItem);
            if (wrappedItem == null) {
                MenuItem wrappedItem2 = MenuWrapperFactory.wrapSupportMenuItem(this.mContext, supportMenuItem);
                this.mMenuItems.put(supportMenuItem, wrappedItem2);
                return wrappedItem2;
            }
            return wrappedItem;
        }
        return menuItem;
    }

    public final SubMenu getSubMenuWrapper(SubMenu subMenu) {
        if (subMenu instanceof SupportSubMenu) {
            SupportSubMenu supportSubMenu = (SupportSubMenu) subMenu;
            if (this.mSubMenus == null) {
                this.mSubMenus = new ArrayMap();
            }
            SubMenu wrappedMenu = this.mSubMenus.get(supportSubMenu);
            if (wrappedMenu == null) {
                SubMenu wrappedMenu2 = MenuWrapperFactory.wrapSupportSubMenu(this.mContext, supportSubMenu);
                this.mSubMenus.put(supportSubMenu, wrappedMenu2);
                return wrappedMenu2;
            }
            return wrappedMenu;
        }
        return subMenu;
    }

    public final void internalClear() {
        Map<SupportMenuItem, MenuItem> map = this.mMenuItems;
        if (map != null) {
            map.clear();
        }
        Map<SupportSubMenu, SubMenu> map2 = this.mSubMenus;
        if (map2 != null) {
            map2.clear();
        }
    }

    public final void internalRemoveGroup(int groupId) {
        Map<SupportMenuItem, MenuItem> map = this.mMenuItems;
        if (map == null) {
            return;
        }
        Iterator<SupportMenuItem> iterator = map.keySet().iterator();
        while (iterator.hasNext()) {
            MenuItem menuItem = iterator.next();
            if (groupId == menuItem.getGroupId()) {
                iterator.remove();
            }
        }
    }

    public final void internalRemoveItem(int id) {
        Map<SupportMenuItem, MenuItem> map = this.mMenuItems;
        if (map == null) {
            return;
        }
        Iterator<SupportMenuItem> iterator = map.keySet().iterator();
        while (iterator.hasNext()) {
            MenuItem menuItem = iterator.next();
            if (id == menuItem.getItemId()) {
                iterator.remove();
                return;
            }
        }
    }
}
