            String report = "NEXUS SAFE CRASHSCREEN\\n"
                    + "Phase: " + phase + "\\n"
                    + "Android SDK: " + Build.VERSION.SDK_INT + "\\n"
                    + "Device: " + Build.MANUFACTURER + " " + Build.MODEL + "\\n\\n"
                    + Log.getStackTraceString(t);
