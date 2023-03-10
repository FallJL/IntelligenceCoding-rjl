package top.hcode.hoj.manager.file;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileReader;
import cn.hutool.core.io.file.FileWriter;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.json.JSONUtil;
import top.hcode.hoj.validator.GroupValidator;
import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.session.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.hcode.hoj.common.exception.StatusFailException;
import top.hcode.hoj.common.exception.StatusForbiddenException;
import top.hcode.hoj.common.result.ResultStatus;
import top.hcode.hoj.manager.oj.ContestCalculateRankManager;
import top.hcode.hoj.pojo.entity.contest.Contest;
import top.hcode.hoj.pojo.entity.contest.ContestPrint;
import top.hcode.hoj.pojo.entity.contest.ContestProblem;
import top.hcode.hoj.pojo.entity.judge.Judge;
import top.hcode.hoj.pojo.vo.ACMContestRankVo;
import top.hcode.hoj.pojo.vo.OIContestRankVo;
import top.hcode.hoj.pojo.vo.UserRolesVo;
import top.hcode.hoj.dao.common.FileEntityService;
import top.hcode.hoj.dao.contest.ContestPrintEntityService;
import top.hcode.hoj.dao.contest.ContestProblemEntityService;
import top.hcode.hoj.dao.contest.ContestEntityService;
import top.hcode.hoj.dao.judge.JudgeEntityService;
import top.hcode.hoj.dao.user.UserInfoEntityService;
import top.hcode.hoj.utils.Constants;
import top.hcode.hoj.validator.ContestValidator;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @Author: Himit_ZH
 * @Date: 2022/3/10 14:27
 * @Description:
 */
@Component
@Slf4j(topic = "hoj")
public class ContestFileManager {

    @Autowired
    private ContestEntityService contestEntityService;

    @Autowired
    private ContestProblemEntityService contestProblemEntityService;

    @Autowired
    private ContestPrintEntityService contestPrintEntityService;

    @Autowired
    private FileEntityService fileEntityService;

    @Autowired
    private JudgeEntityService judgeEntityService;

    @Autowired
    private UserInfoEntityService userInfoEntityService;

    @Autowired
    private ContestCalculateRankManager contestCalculateRankManager;

    @Autowired
    private ContestValidator contestValidator;

    @Autowired
    private GroupValidator groupValidator;

    public void downloadContestRank(Long cid, Boolean forceRefresh, Boolean removeStar, HttpServletResponse response) throws IOException, StatusFailException, StatusForbiddenException {
        // ???????????????????????????
        Session session = SecurityUtils.getSubject().getSession();
        UserRolesVo userRolesVo = (UserRolesVo) session.getAttribute("userInfo");

        // ???????????????????????????
        Contest contest = contestEntityService.getById(cid);

        if (contest == null) {
            throw new StatusFailException("??????????????????????????????");
        }

        // ????????????????????????
        boolean isRoot = SecurityUtils.getSubject().hasRole("root");

        Long gid = contest.getGid();

        if (!isRoot
                && !contest.getUid().equals(userRolesVo.getUid())
                && !(contest.getIsGroup() && groupValidator.isGroupRoot(userRolesVo.getUid(), gid))) {
            throw new StatusForbiddenException("???????????????????????????????????????????????????????????????");
        }

        // ????????????????????????????????????
        Boolean isOpenSealRank = contestValidator.isSealRank(userRolesVo.getUid(), contest, forceRefresh, isRoot);
        response.setContentType("application/vnd.ms-excel");
        response.setCharacterEncoding("utf-8");
        // ??????URLEncoder.encode????????????????????????
        String fileName = URLEncoder.encode("contest_" + contest.getId() + "_rank", "UTF-8");
        response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");
        response.setHeader("Content-Type", "application/xlsx");

        // ????????????displayID??????
        QueryWrapper<ContestProblem> contestProblemQueryWrapper = new QueryWrapper<>();
        contestProblemQueryWrapper.eq("cid", contest.getId()).select("display_id").orderByAsc("display_id");
        List<String> contestProblemDisplayIDList = contestProblemEntityService.list(contestProblemQueryWrapper)
                .stream().map(ContestProblem::getDisplayId).collect(Collectors.toList());

        if (contest.getType().intValue() == Constants.Contest.TYPE_ACM.getCode()) { // ACM??????

            List<ACMContestRankVo> acmContestRankVoList = contestCalculateRankManager.calcACMRank(
                    isOpenSealRank,
                    removeStar,
                    contest,
                    null,
                    null);
            EasyExcel.write(response.getOutputStream())
                    .head(fileEntityService.getContestRankExcelHead(contestProblemDisplayIDList, true))
                    .sheet("rank")
                    .doWrite(fileEntityService.changeACMContestRankToExcelRowList(acmContestRankVoList, contestProblemDisplayIDList, contest.getRankShowName()));
        } else {
            List<OIContestRankVo> oiContestRankVoList = contestCalculateRankManager.calcOIRank(
                    isOpenSealRank,
                    removeStar,
                    contest,
                    null,
                    null);
            EasyExcel.write(response.getOutputStream())
                    .head(fileEntityService.getContestRankExcelHead(contestProblemDisplayIDList, false))
                    .sheet("rank")
                    .doWrite(fileEntityService.changOIContestRankToExcelRowList(oiContestRankVoList, contestProblemDisplayIDList, contest.getRankShowName()));
        }
    }

    public void downloadContestACSubmission(Long cid, Boolean excludeAdmin, String splitType, HttpServletResponse response) throws StatusForbiddenException, StatusFailException {

        Contest contest = contestEntityService.getById(cid);

        if (contest == null) {
            throw new StatusFailException("??????????????????????????????");
        }

        // ???????????????????????????
        Session session = SecurityUtils.getSubject().getSession();
        UserRolesVo userRolesVo = (UserRolesVo) session.getAttribute("userInfo");
        boolean isRoot = SecurityUtils.getSubject().hasRole("root");
        // ?????????root ??????????????????????????????????????????ac??????

        Long gid = contest.getGid();
        if (!isRoot
                && !contest.getUid().equals(userRolesVo.getUid())
                && !(contest.getIsGroup() && groupValidator.isGroupRoot(userRolesVo.getUid(), gid))) {
            throw new StatusForbiddenException("??????????????????????????????????????????????????????AC?????????");
        }

        boolean isACM = contest.getType().intValue() == Constants.Contest.TYPE_ACM.getCode();

        QueryWrapper<ContestProblem> contestProblemQueryWrapper = new QueryWrapper<>();
        contestProblemQueryWrapper.eq("cid", contest.getId());
        List<ContestProblem> contestProblemList = contestProblemEntityService.list(contestProblemQueryWrapper);

        List<String> superAdminUidList = userInfoEntityService.getSuperAdminUidList();

        QueryWrapper<Judge> judgeQueryWrapper = new QueryWrapper<>();
        judgeQueryWrapper.eq("cid", cid)
                .eq(isACM, "status", Constants.Judge.STATUS_ACCEPTED.getStatus())
                .isNotNull(!isACM, "score") // OI?????????????????????null???
                .between("submit_time", contest.getStartTime(), contest.getEndTime())
                .ne(excludeAdmin, "uid", contest.getUid()) // ????????????????????????root
                .notIn(excludeAdmin && superAdminUidList.size() > 0, "uid", superAdminUidList)
                .orderByDesc("submit_time");

        List<Judge> judgeList = judgeEntityService.list(judgeQueryWrapper);

        // ??????????????????????????? -> username??????????????????
        String tmpFilesDir = Constants.File.CONTEST_AC_SUBMISSION_TMP_FOLDER.getPath() + File.separator + IdUtil.fastSimpleUUID();
        FileUtil.mkdir(tmpFilesDir);

        HashMap<String, Boolean> recordMap = new HashMap<>();
        if ("user".equals(splitType)) {
            /**
             * ?????????????????????????????????
             */
            List<String> usernameList = judgeList.stream()
                    .filter(distinctByKey(Judge::getUsername)) // ???????????????????????????
                    .map(Judge::getUsername).collect(Collectors.toList()); // ????????????????????????


            HashMap<Long, String> cpIdMap = new HashMap<>();
            for (ContestProblem contestProblem : contestProblemList) {
                cpIdMap.put(contestProblem.getId(), contestProblem.getDisplayId());
            }

            for (String username : usernameList) {
                // ??????????????????????????????????????????
                String userDir = tmpFilesDir + File.separator + username;
                FileUtil.mkdir(userDir);
                // ?????????ACM????????????????????????????????????????????????????????????????????????AC?????????????????????????????? ---> A_(666666).c
                // ?????????OI????????????????????????????????????????????????????????? ---> A_(666666)_100.c
                List<Judge> userSubmissionList = judgeList.stream()
                        .filter(judge -> judge.getUsername().equals(username)) // ??????????????????????????????
                        .sorted(Comparator.comparing(Judge::getSubmitTime).reversed()) // ??????????????????????????????
                        .collect(Collectors.toList());

                for (Judge judge : userSubmissionList) {
                    String filePath = userDir + File.separator + cpIdMap.getOrDefault(judge.getCpid(), "null");

                    // OI??????????????????????????????
                    if (!isACM) {
                        String key = judge.getUsername() + "_" + judge.getPid();
                        if (!recordMap.containsKey(key)) {
                            filePath += "_" + judge.getScore() + "_(" + threadLocalTime.get().format(judge.getSubmitTime()) + ")."
                                    + languageToFileSuffix(judge.getLanguage().toLowerCase());
                            FileWriter fileWriter = new FileWriter(filePath);
                            fileWriter.write(judge.getCode());
                            recordMap.put(key, true);
                        }

                    } else {
                        filePath += "_(" + threadLocalTime.get().format(judge.getSubmitTime()) + ")."
                                + languageToFileSuffix(judge.getLanguage().toLowerCase());
                        FileWriter fileWriter = new FileWriter(filePath);
                        fileWriter.write(judge.getCode());
                    }

                }
            }
        } else if ("problem".equals(splitType)) {
            /**
             * ?????????????????????????????????????????????
             */

            for (ContestProblem contestProblem : contestProblemList) {
                // ???????????????????????????????????????
                String problemDir = tmpFilesDir + File.separator + contestProblem.getDisplayId();
                FileUtil.mkdir(problemDir);
                // ?????????ACM????????????????????????????????????????????????????????????????????????AC?????????????????????????????? ---> username_(666666).c
                // ?????????OI????????????????????????????????????????????????????????? ---> username_(666666)_100.c
                List<Judge> problemSubmissionList = judgeList.stream()
                        .filter(judge -> judge.getPid().equals(contestProblem.getPid())) // ??????????????????????????????
                        .sorted(Comparator.comparing(Judge::getSubmitTime).reversed()) // ??????????????????????????????
                        .collect(Collectors.toList());

                for (Judge judge : problemSubmissionList) {
                    String filePath = problemDir + File.separator + judge.getUsername();
                    if (!isACM) {
                        String key = judge.getUsername() + "_" + contestProblem.getDisplayId();
                        // OI??????????????????????????????
                        if (!recordMap.containsKey(key)) {
                            filePath += "_" + judge.getScore() + "_(" + threadLocalTime.get().format(judge.getSubmitTime()) + ")."
                                    + languageToFileSuffix(judge.getLanguage().toLowerCase());
                            FileWriter fileWriter = new FileWriter(filePath);
                            fileWriter.write(judge.getCode());
                            recordMap.put(key, true);
                        }
                    } else {
                        filePath += "_(" + threadLocalTime.get().format(judge.getSubmitTime()) + ")."
                                + languageToFileSuffix(judge.getLanguage().toLowerCase());
                        FileWriter fileWriter = new FileWriter(filePath);
                        fileWriter.write(judge.getCode());
                    }
                }
            }
        }

        String zipFileName = "contest_" + contest.getId() + "_" + System.currentTimeMillis() + ".zip";
        String zipPath = Constants.File.CONTEST_AC_SUBMISSION_TMP_FOLDER.getPath() + File.separator + zipFileName;
        ZipUtil.zip(tmpFilesDir, zipPath);
        // ???zip??????io??????????????????
        FileReader zipFileReader = new FileReader(zipPath);
        BufferedInputStream bins = new BufferedInputStream(zipFileReader.getInputStream());//?????????????????????
        OutputStream outs = null;//??????????????????IO???
        BufferedOutputStream bouts = null;
        try {
            outs = response.getOutputStream();
            bouts = new BufferedOutputStream(outs);
            response.setContentType("application/x-download");
            response.setHeader("Content-disposition", "attachment;filename=" + URLEncoder.encode(zipFileName, "UTF-8"));
            int bytesRead = 0;
            byte[] buffer = new byte[1024 * 10];
            //??????????????????????????????
            while ((bytesRead = bins.read(buffer, 0, 1024 * 10)) != -1) {
                bouts.write(buffer, 0, bytesRead);
            }
            // ????????????
            bouts.flush();
        } catch (IOException e) {
            log.error("????????????AC?????????????????????????????????------------>", e);
            response.reset();
            response.setContentType("application/json");
            response.setCharacterEncoding("utf-8");
            Map<String, Object> map = new HashMap<>();
            map.put("status", ResultStatus.SYSTEM_ERROR);
            map.put("msg", "???????????????????????????????????????");
            map.put("data", null);
            try {
                response.getWriter().println(JSONUtil.toJsonStr(map));
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        } finally {
            try {
                bins.close();
                if (outs != null) {
                    outs.close();
                }
                if (bouts != null) {
                    bouts.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        FileUtil.del(tmpFilesDir);
        FileUtil.del(zipPath);

    }

    public void downloadContestPrintText(Long id, HttpServletResponse response) throws StatusForbiddenException {
        ContestPrint contestPrint = contestPrintEntityService.getById(id);
        Session session = SecurityUtils.getSubject().getSession();
        UserRolesVo userRolesVo = (UserRolesVo) session.getAttribute("userInfo");
        boolean isRoot = SecurityUtils.getSubject().hasRole("root");

        Long cid = contestPrint.getCid();

        Contest contest = contestEntityService.getById(cid);

        Long gid = contest.getGid();

        if (!isRoot && !contest.getUid().equals(userRolesVo.getUid())
                && !(contest.getIsGroup() && groupValidator.isGroupRoot(userRolesVo.getUid(), gid))) {
            throw new StatusForbiddenException("?????????????????????????????????????????????????????????????????????");
        }

        String filename = contestPrint.getUsername() + "_Contest_Print.txt";
        String filePath = Constants.File.CONTEST_TEXT_PRINT_FOLDER.getPath() + File.separator + id + File.separator + filename;
        if (!FileUtil.exist(filePath)) {

            FileWriter fileWriter = new FileWriter(filePath);
            fileWriter.write(contestPrint.getContent());
        }

        FileReader zipFileReader = new FileReader(filePath);
        BufferedInputStream bins = new BufferedInputStream(zipFileReader.getInputStream());//?????????????????????
        OutputStream outs = null;//??????????????????IO???
        BufferedOutputStream bouts = null;
        try {
            outs = response.getOutputStream();
            bouts = new BufferedOutputStream(outs);
            response.setContentType("application/x-download");
            response.setHeader("Content-disposition", "attachment;filename=" + URLEncoder.encode(filename, "UTF-8"));
            int bytesRead = 0;
            byte[] buffer = new byte[1024 * 10];
            //??????????????????????????????
            while ((bytesRead = bins.read(buffer, 0, 1024 * 10)) != -1) {
                bouts.write(buffer, 0, bytesRead);
            }
            // ????????????
            bouts.flush();
        } catch (IOException e) {
            log.error("????????????????????????????????????------------>", e);
            response.reset();
            response.setContentType("application/json");
            response.setCharacterEncoding("utf-8");
            Map<String, Object> map = new HashMap<>();
            map.put("status", ResultStatus.SYSTEM_ERROR);
            map.put("msg", "???????????????????????????????????????");
            map.put("data", null);
            try {
                response.getWriter().println(JSONUtil.toJsonStr(map));
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        } finally {
            try {
                bins.close();
                if (outs != null) {
                    outs.close();
                }
                if (bouts != null) {
                    bouts.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private static final ThreadLocal<SimpleDateFormat> threadLocalTime = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyyMMddHHmmss");
        }
    };

    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

    private static String languageToFileSuffix(String language) {

        List<String> CLang = Arrays.asList("c", "gcc", "clang");
        List<String> CPPLang = Arrays.asList("c++", "g++", "clang++");
        List<String> PythonLang = Arrays.asList("python", "pypy");

        for (String lang : CPPLang) {
            if (language.contains(lang)) {
                return "cpp";
            }
        }

        if (language.contains("c#")) {
            return "cs";
        }

        for (String lang : CLang) {
            if (language.contains(lang)) {
                return "c";
            }
        }

        for (String lang : PythonLang) {
            if (language.contains(lang)) {
                return "py";
            }
        }

        if (language.contains("javascript")) {
            return "js";
        }

        if (language.contains("java")) {
            return "java";
        }

        if (language.contains("pascal")) {
            return "pas";
        }

        if (language.contains("go")) {
            return "go";
        }

        if (language.contains("php")) {
            return "php";
        }

        return "txt";
    }

}