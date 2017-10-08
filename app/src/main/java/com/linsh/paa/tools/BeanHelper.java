package com.linsh.paa.tools;

import com.linsh.lshutils.utils.Basic.LshStringUtils;
import com.linsh.paa.model.bean.db.Item;
import com.linsh.paa.model.bean.db.ItemHistory;
import com.linsh.paa.model.bean.json.TaobaoDetail;

/**
 * <pre>
 *    author : Senh Linsh
 *    date   : 2017/10/03
 *    desc   :
 * </pre>
 */
public class BeanHelper {

    public static Object[] getItemAndHistiryToSave(Item item, TaobaoDetail detail) {
        if (detail.isSuccess()) {
            ItemHistory history = null;
            Item itemCopy = null;
            if (item == null) {
                history = new ItemHistory(detail.getItemId());
                itemCopy = new Item(detail.getItemId());
                itemCopy.setPrice(detail.getItemPrice());
                history.setPrice(detail.getItemPrice());
                itemCopy.setTitle(detail.getItemTitle());
                history.setTitle(detail.getItemTitle());
                itemCopy.setImage(detail.getItemImage());
                itemCopy.setShopName(detail.getShopName());
            } else if (item.getId().equals(detail.getItemId())) {
                history = new ItemHistory(item.getId());
                if (!LshStringUtils.isEquals(item.getPrice(), detail.getItemPrice())) {
                    itemCopy = item.getCopy();
                    itemCopy.setPrice(detail.getItemPrice());
                    history.setPrice(detail.getItemPrice());
                }
                if (!LshStringUtils.isEquals(item.getTitle(), detail.getItemTitle())) {
                    if (itemCopy == null) itemCopy = item.getCopy();
                    itemCopy.setTitle(detail.getItemTitle());
                    history.setTitle(detail.getItemTitle());
                }
                if (!LshStringUtils.isEquals(item.getImage(), detail.getItemImage())) {
                    if (itemCopy == null) itemCopy = item.getCopy();
                    itemCopy.setImage(detail.getItemImage());
                }
                if (!LshStringUtils.isEquals(item.getShopName(), detail.getShopName())) {
                    if (itemCopy == null) itemCopy = item.getCopy();
                    itemCopy.setShopName(detail.getShopName());
                }
            }
            return new Object[]{check(itemCopy), history};
        }
        return null;
    }

    private static Item check(Item item) {
        if (item == null || LshStringUtils.isEmpty(item.getId())
                || LshStringUtils.isEmpty(item.getTitle()) || LshStringUtils.isEmpty(item.getPrice()))
            return null;
        return item;
    }

    public static boolean isSame(ItemHistory history1, ItemHistory history2) {
        if (history1 != null && history2 != null) {
            return LshStringUtils.isEquals(history1.getId(), history2.getId())
                    && LshStringUtils.isEquals(history1.getTitle(), history2.getTitle())
                    && LshStringUtils.isEquals(history1.getPrice(), history2.getPrice())
                    && history1.isBuyDisable() == history2.isBuyDisable();
        }
        return false;
    }
}
