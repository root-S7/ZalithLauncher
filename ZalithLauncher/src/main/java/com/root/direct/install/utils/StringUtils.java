package com.root.direct.install.utils;

import java.util.*;
import java.util.stream.*;

public class StringUtils {

    /**
     * 检查给定对象数组是否有效
     * 有效条件：
     *   1.数组本身不为null
     *   2.数组内所有元素都不为null
     *   3.如果元素是字符串，则trim后不能全是空白字符
     *
     * @param o 可变参数对象数组
     * @return 只要有一个元素不合法都返回false
    **/
    public static boolean isEffective(Object... o) {
        return Optional.ofNullable(o)
                .map(Stream::of)
                .orElseGet(Stream::empty)
                .allMatch(i -> i != null && (!(i instanceof String) || !((String) i).trim().isBlank()));
    }
}
