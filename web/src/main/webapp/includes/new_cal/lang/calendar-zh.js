// ** I18N

// Calendar ZH (Simplified Chinese) language
// Chinese UI text maintained by Clinica EDC. Original legal notices retained.
// Author: Mihai Bazon, <mihai_bazon@yahoo.com>
// Encoding: any
// Distributed under the same terms as the calendar itself.

// For translators: please use UTF-8 if possible.  We strongly believe that
// Unicode is the answer to a real internationalized world.  Also please
// include your contact information in the header, as can be seen above.

// full day names
Calendar._DN = new Array
("\u661f\u671f\u65e5",
 "\u661f\u671f\u4e00",
 "\u661f\u671f\u4e8c",
 "\u661f\u671f\u4e09",
 "\u661f\u671f\u56db",
 "\u661f\u671f\u4e94",
 "\u661f\u671f\u516d",
 "\u661f\u671f\u65e5");

// Please note that the following array of short day names (and the same goes
// for short month names, _SMN) isn't absolutely necessary.  We give it here
// for exemplification on how one can customize the short day names, but if
// they are simply the first N letters of the full name you can simply say:
//
//   Calendar._SDN_len = N; // short day name length
//   Calendar._SMN_len = N; // short month name length
//
// If N = 3 then this is not needed either since we assume a value of 3 if not
// present, to be compatible with translation files that were written before
// this feature.

// short day names
Calendar._SDN = new Array
("\u65e5",
 "\u4e00",
 "\u4e8c",
 "\u4e09",
 "\u56db",
 "\u4e94",
 "\u516d",
 "\u65e5");

// First day of the week. "0" means display Sunday first, "1" means display
// Monday first, etc.
Calendar._FD = 0;

// full month names
Calendar._MN = new Array
("1\u6708",
 "2\u6708",
 "3\u6708",
 "4\u6708",
 "5\u6708",
 "6\u6708",
 "7\u6708",
 "8\u6708",
 "9\u6708",
 "10\u6708",
 "11\u6708",
 "12\u6708");

// Locale-native MMM tokens match Java Locale.CHINESE; canonical patterns remain unchanged.
// short month names
Calendar._SMN = new Array
("1\u6708",
 "2\u6708",
 "3\u6708",
 "4\u6708",
 "5\u6708",
 "6\u6708",
 "7\u6708",
 "8\u6708",
 "9\u6708",
 "10\u6708",
 "11\u6708",
 "12\u6708");

// tooltips
Calendar._TT = {};
Calendar._TT["INFO"] = "\u5173\u4e8e\u65e5\u5386";

Calendar._TT["ABOUT"] =
"DHTML Date/Time Selector\n" +
"(c) dynarch.com 2002-2005 / Author: Mihai Bazon\n" + // don't translate this this ;-)
"For latest version visit: http://www.dynarch.com/projects/calendar/\n" +
"Distributed under GNU LGPL.  See http://gnu.org/licenses/lgpl.html for details." +
"\n\n" +
"\u65e5\u671f\u9009\u62e9\uff1a\n" +
"- \u4f7f\u7528 \xab\u3001\xbb \u6309\u94ae\u9009\u62e9\u5e74\u4efd\n" +
"- \u4f7f\u7528 " + String.fromCharCode(0x2039) + "\u3001" + String.fromCharCode(0x203a) + " \u6309\u94ae\u9009\u62e9\u6708\u4efd\n" +
"- \u6309\u4f4f\u4e0a\u8ff0\u6309\u94ae\u53ef\u5feb\u901f\u9009\u62e9\u3002";
Calendar._TT["ABOUT_TIME"] = "\n\n" +
"\u65f6\u95f4\u9009\u62e9\uff1a\n" +
"- \u5355\u51fb\u65f6\u95f4\u7684\u4efb\u4e00\u90e8\u5206\u4ee5\u589e\u52a0\u6570\u503c\n" +
"- \u6309\u4f4fShift\u952e\u5355\u51fb\u4ee5\u51cf\u5c11\u6570\u503c\n" +
"- \u5355\u51fb\u5e76\u62d6\u52a8\u53ef\u5feb\u901f\u8c03\u6574\u3002";

Calendar._TT["PREV_YEAR"] = "\u4e0a\u4e00\u5e74\uff08\u6309\u4f4f\u663e\u793a\u83dc\u5355\uff09";
Calendar._TT["PREV_MONTH"] = "\u4e0a\u4e2a\u6708\uff08\u6309\u4f4f\u663e\u793a\u83dc\u5355\uff09";
Calendar._TT["GO_TODAY"] = "\u8f6c\u5230\u4eca\u5929";
Calendar._TT["NEXT_MONTH"] = "\u4e0b\u4e2a\u6708\uff08\u6309\u4f4f\u663e\u793a\u83dc\u5355\uff09";
Calendar._TT["NEXT_YEAR"] = "\u4e0b\u4e00\u5e74\uff08\u6309\u4f4f\u663e\u793a\u83dc\u5355\uff09";
Calendar._TT["SEL_DATE"] = "\u9009\u62e9\u65e5\u671f";
Calendar._TT["DRAG_TO_MOVE"] = "\u62d6\u52a8\u4ee5\u79fb\u52a8";
Calendar._TT["PART_TODAY"] = "\uff08\u4eca\u5929\uff09";

// the following is to inform that "%s" is to be the first day of week
// %s will be replaced with the day name.
Calendar._TT["DAY_FIRST"] = "\u5c06%s\u8bbe\u4e3a\u6bcf\u5468\u9996\u65e5";

// This may be locale-dependent.  It specifies the week-end days, as an array
// of comma-separated numbers.  The numbers are from 0 to 6: 0 means Sunday, 1
// means Monday, etc.
Calendar._TT["WEEKEND"] = "0,6";

Calendar._TT["CLOSE"] = "\u5173\u95ed";
Calendar._TT["TODAY"] = "\u4eca\u5929";
Calendar._TT["TIME_PART"] = "\u5355\u51fb\u6216\u62d6\u52a8\u4ee5\u66f4\u6539\u6570\u503c\uff08Shift\u952e\u53cd\u5411\u8c03\u6574\uff09";

// date formats
Calendar._TT["DEF_DATE_FORMAT"] = "%Y-%m-%d";
Calendar._TT["TT_DATE_FORMAT"] = "%a, %b %e";

Calendar._TT["WK"] = "\u5468";
Calendar._TT["TIME"] = "\u65f6\u95f4\uff1a";
