package com.knightboost.cpuprofiler.core;

public class ProcConst {

    public static final int PROC_TERM_MASK = 0xff;

    public static final int PROC_ZERO_TERM = 0;

    public static final int PROC_SPACE_TERM = (int)' ';

    public static final int PROC_TAB_TERM = (int)'\t';

    public static final int PROC_NEWLINE_TERM = (int) '\n';

    public static final int PROC_COMBINE = 0x100;

    public static final int PROC_PARENS = 0x200;

    public static final int PROC_QUOTES = 0x400;

    public static final int PROC_CHAR = 0x800;

    public static final int PROC_OUT_STRING = 0x1000;

    public static final int PROC_OUT_LONG = 0x2000;

    public static final int PROC_OUT_FLOAT = 0x4000;
}
