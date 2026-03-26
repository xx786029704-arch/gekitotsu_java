package org.example;

import org.example.elements.*;
import org.example.elements.hit.KekkaiField;
import org.example.elements.units.*;
import org.example.elements.wall.*;

import java.util.*;

public class Settings {
    public static final boolean ENABLE_VISUALIZATION = true;    //是否开启可视化
    public static final boolean SHOW_UNIT_HP = true;    // 是否显示单位生命值（用于debug）
    public static final int LOGIC_TPS = 0;      //帧率限制，0代表无限制
    public static final int max_run_time = 65536;    //最大运行帧数
}
