package top.hcode.hoj.manager.admin.rejudge;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import top.hcode.hoj.common.exception.StatusFailException;
import top.hcode.hoj.dao.contest.ContestRecordEntityService;
import top.hcode.hoj.dao.judge.JudgeCaseEntityService;
import top.hcode.hoj.dao.judge.JudgeEntityService;
import top.hcode.hoj.dao.problem.ProblemEntityService;
import top.hcode.hoj.dao.user.UserAcproblemEntityService;
import top.hcode.hoj.judge.remote.RemoteJudgeDispatcher;
import top.hcode.hoj.judge.self.JudgeDispatcher;
import top.hcode.hoj.pojo.entity.contest.ContestRecord;
import top.hcode.hoj.pojo.entity.judge.Judge;
import top.hcode.hoj.pojo.entity.judge.JudgeCase;
import top.hcode.hoj.pojo.entity.problem.Problem;
import top.hcode.hoj.pojo.entity.user.UserAcproblem;
import top.hcode.hoj.utils.Constants;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * @Author: Himit_ZH
 * @Date: 2022/3/9 16:21
 * @Description:
 */
@Component
public class RejudgeManager {

    @Resource
    private JudgeEntityService judgeEntityService;

    @Resource
    private UserAcproblemEntityService userAcproblemEntityService;

    @Resource
    private ContestRecordEntityService contestRecordEntityService;

    @Resource
    private JudgeCaseEntityService judgeCaseEntityService;

    @Resource
    private ProblemEntityService problemEntityService;

    @Resource
    private JudgeDispatcher judgeDispatcher;

    @Resource
    private RemoteJudgeDispatcher remoteJudgeDispatcher;

    @Transactional(rollbackFor = Exception.class)
    public  Judge rejudge(Long submitId) throws StatusFailException {
        Judge judge = judgeEntityService.getById(submitId);

        boolean isContestSubmission = judge.getCid() != 0;
        boolean resetContestRecordResult = true;

        // ????????????????????????
        if (!isContestSubmission) {
            // ?????????????????????????????????????????????????????????
            // ?????????????????????AC???????????????????????????????????????ac????????? user_acproblem
            if (judge.getStatus().intValue() == Constants.Judge.STATUS_ACCEPTED.getStatus().intValue()) {
                QueryWrapper<UserAcproblem> userAcproblemQueryWrapper = new QueryWrapper<>();
                userAcproblemQueryWrapper.eq("submit_id", judge.getSubmitId());
                userAcproblemEntityService.remove(userAcproblemQueryWrapper);
            }
        } else {
            // ???????????????????????????????????????
            UpdateWrapper<ContestRecord> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("submit_id", submitId).setSql("status=null,score=null");
            resetContestRecordResult = contestRecordEntityService.update(updateWrapper);
        }

        // ???????????????????????????????????????
        QueryWrapper<JudgeCase> judgeCaseQueryWrapper = new QueryWrapper<>();
        judgeCaseQueryWrapper.eq("submit_id", submitId);
        judgeCaseEntityService.remove(judgeCaseQueryWrapper);

        boolean hasSubmitIdRemoteRejudge = isHasSubmitIdRemoteRejudge(judge.getVjudgeSubmitId(), judge.getStatus());

        // ???????????????
        judge.setStatus(Constants.Judge.STATUS_PENDING.getStatus()); // ????????????????????????
        judge.setVersion(judge.getVersion() + 1);
        judge.setJudger("")
                .setTime(null)
                .setMemory(null)
                .setErrorMessage(null)
                .setOiRankScore(null)
                .setScore(null);

        boolean result = judgeEntityService.updateById(judge);


        if (result && resetContestRecordResult) {
            // ??????????????????
            Problem problem = problemEntityService.getById(judge.getPid());
            if (problem.getIsRemote()) { // ???????????????oj??????
                remoteJudgeDispatcher.sendTask(judge, problem.getProblemId(),
                        isContestSubmission, hasSubmitIdRemoteRejudge);
            } else {
                judgeDispatcher.sendTask(judge, isContestSubmission);
            }
            return judge;
        } else {
            throw new StatusFailException("?????????????????????????????????");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void rejudgeContestProblem(Long cid, Long pid) throws StatusFailException {
        QueryWrapper<Judge> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("cid", cid).eq("pid", pid);
        List<Judge> rejudgeList = judgeEntityService.list(queryWrapper);

        if (rejudgeList.size() == 0) {
            throw new StatusFailException("??????????????????????????????????????????");
        }

        List<Long> submitIdList = new LinkedList<>();
        HashMap<Long, Integer> idMapStatus = new HashMap<>();
        // ?????????????????????
        for (Judge judge : rejudgeList) {
            idMapStatus.put(judge.getSubmitId(), judge.getStatus());
            judge.setStatus(Constants.Judge.STATUS_PENDING.getStatus()); // ????????????????????????
            judge.setVersion(judge.getVersion() + 1);
            judge.setJudger("")
                    .setTime(null)
                    .setMemory(null)
                    .setErrorMessage(null)
                    .setOiRankScore(null)
                    .setScore(null);
            submitIdList.add(judge.getSubmitId());
        }
        boolean resetJudgeResult = judgeEntityService.updateBatchById(rejudgeList);
        // ??????????????????????????????????????????
        QueryWrapper<JudgeCase> judgeCaseQueryWrapper = new QueryWrapper<>();
        judgeCaseQueryWrapper.in("submit_id", submitIdList);
        judgeCaseEntityService.remove(judgeCaseQueryWrapper);
        // ???????????????????????????????????????
        UpdateWrapper<ContestRecord> updateWrapper = new UpdateWrapper<>();
        updateWrapper.in("submit_id", submitIdList).setSql("status=null,score=null");
        boolean resetContestRecordResult = contestRecordEntityService.update(updateWrapper);

        if (resetContestRecordResult && resetJudgeResult) {
            // ??????????????????
            Problem problem = problemEntityService.getById(pid);
            if (problem.getIsRemote()) { // ???????????????oj??????
                for (Judge judge : rejudgeList) {
                    // ?????????????????????????????????????????????
                    remoteJudgeDispatcher.sendTask(judge, problem.getProblemId(),
                            judge.getCid() != 0,
                            isHasSubmitIdRemoteRejudge(judge.getVjudgeSubmitId(), idMapStatus.get(judge.getSubmitId())));
                }
            } else {
                for (Judge judge : rejudgeList) {
                    // ?????????????????????????????????????????????
                    judgeDispatcher.sendTask(judge, judge.getCid() != 0);
                }
            }
        } else {
            throw new StatusFailException("?????????????????????????????????");
        }
    }

    private boolean isHasSubmitIdRemoteRejudge(Long vjudgeSubmitId, int status) {
        boolean isHasSubmitIdRemoteRejudge = false;
        if (vjudgeSubmitId != null &&
                (status == Constants.Judge.STATUS_SUBMITTED_FAILED.getStatus()
                        || status == Constants.Judge.STATUS_COMPILING.getStatus()
                        || status == Constants.Judge.STATUS_PENDING.getStatus()
                        || status == Constants.Judge.STATUS_JUDGING.getStatus()
                        || status == Constants.Judge.STATUS_SYSTEM_ERROR.getStatus())) {
            isHasSubmitIdRemoteRejudge = true;
        }
        return isHasSubmitIdRemoteRejudge;
    }
}