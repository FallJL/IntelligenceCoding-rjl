import moment from 'moment'
import api from '@/common/api'
import { EXAM_STATUS, EXAM_TYPE } from '@/common/constants'
import time from '@/common/time'

const state = { //state存放全局共享的数据
    now: moment(),
    intoAccess: false, // 考试进入权限
    submitAccess:false, // 保护考试的提交权限
    forceUpdate: false, // 强制实时榜单
    removeStar: false, // 榜单去除打星队伍
    concernedList:[], // 关注队伍
    exam: {
        auth: EXAM_TYPE.PUBLIC,
        openPrint: false,
        rankShowName:'username',
    },
    examProblems: [],
    itemVisible: {
        table: true,
        chart: true,
    },
    disPlayIdMapColor:{}, // 展示id对应的气球颜色
    groupExamAuth: 0,
}

const getters = {//通过对state中数据进行加工处理形成新的数据，类似于vue中的计算属性，store中数据改变，getter随之改变
    //(1)通过this.$store.getter.对应函数名直接调用
    //(2)...mapGetter(['对应函数名'])
    examStatus: (state, getters) => {
        return state.exam.status;
    },
    examRuleType: (state,getters) => {
        return state.exam.type;
    },
    isExamAdmin: (state, getters, _, rootGetters) => {
        return rootGetters.isAuthenticated &&
            (state.exam.author === rootGetters.userInfo.username || rootGetters.isSuperAdmin || state.groupExamAuth == 5)
    },
    canSubmit:(state, getters)=>{
        return state.intoAccess||state.submitAccess || state.exam.auth === EXAM_TYPE.PUBLIC ||getters.isExamAdmin
    },
    examMenuDisabled: (state, getters) => {
        // 考试创建者或者超级管理员可以直接查看
        if (getters.isExamAdmin) return false
        // 未开始不可查看
        if(getters.examStatus === EXAM_STATUS.SCHEDULED) return true

        if (state.exam.auth === EXAM_TYPE.PRIVATE) {
            // 私有赛需要通过验证密码方可查看考试
            return !state.intoAccess
        }

    },

    // 榜单是否实时刷新
    ExamRealTimePermission: (state, getters, _, rootGetters) => {
        // 考试若是已结束，便是最后榜单
        if (getters.examStatus === EXAM_STATUS.ENDED) {
            return true
        }
        // 考试管理员直接可看到实时榜单
        if(getters.isExamAdmin){
            return true
        }
        // 考试是否开启
        if(state.exam.sealRank === true){
            // 当前时间在封榜时间之后，即不刷新榜单
            return !state.now.isAfter(moment(state.exam.sealRankTime))
        }else{
            return true
        }
    },
    problemSubmitDisabled: (state, getters, _, rootGetters) => {
        // 考试结束不可交题
        if (getters.examStatus === EXAM_STATUS.ENDED) {
            return true

            // 考试未开始不可交题，除非是考试管理者
        } else if (getters.examStatus === EXAM_STATUS.SCHEDULED) {
            return !getters.isExamAdmin
        }
        // 未登录不可交题
        return !rootGetters.isAuthenticated
    },
    // 是否需要显示密码验证框
    passwordFormVisible: (state, getters) => {
        // 如果是公开赛，保护赛，或已注册过，管理员都不用再显示
        return state.exam.auth !== EXAM_TYPE.PUBLIC &&state.exam.auth !== EXAM_TYPE.PROTECTED &&!state.intoAccess && !getters.isExamAdmin
    },
    examStartTime: (state) => {
        return moment(state.exam.startTime)
    },
    examEndTime: (state) => {
        return moment(state.exam.endTime)
    },
    // 考试计时文本显示
    countdown: (state, getters) => {
        // 还未开始的显示
        if (getters.examStatus === EXAM_STATUS.SCHEDULED) {

            let durationMs = getters.examStartTime.diff(state.now, 'seconds')

            let duration = moment.duration(durationMs, 'seconds')
            // time is too long
            if (duration.weeks() > 0) {
                return 'Start At ' + duration.humanize()
            }

            if(duration.asSeconds()<=0){
                state.exam.status = EXAM_STATUS.RUNNING
            }

            let texts = time.secondFormat(durationMs)
            return '-' + texts
            // 考试进行中的显示
        } else if (getters.examStatus === EXAM_STATUS.RUNNING) {
            // 倒计时文本显示
            if(getters.examEndTime.diff(state.now, 'seconds')>0){
                let texts = time.secondFormat(getters.examEndTime.diff(state.now, 'seconds'))
                return '-' + texts
            }else{
                state.exam.status = EXAM_STATUS.ENDED
                return "00:00:00"
            }

        } else {
            return 'Ended'
        }
    },
    // 考试开始到现在经过的秒数
    BeginToNowDuration:(state,getters)=>{
        return moment.duration(state.now.diff(getters.examStartTime, 'seconds'), 'seconds').asSeconds()
    },

    // 考试进度条显示
    progressValue:(state,getters)=>{
        // 还未开始的显示
        if (getters.examStatus === EXAM_STATUS.SCHEDULED) {
            return 0;
            // 考试进行中的显示
        } else if (getters.examStatus === EXAM_STATUS.RUNNING) {
            // 获取考试开始到现在经过的秒数
            let duration = getters.BeginToNowDuration
            // 消耗时间除以整体时间
            return (duration / state.exam.duration)*100
        }else{
            return 100;
        }
    },
}

const mutations = {//用于变更store中的数据，不允许异步代码
    //（1）可以在methods中通过this.$store.commit('对应函数名')来调用对应函数
    //（2）也可以...mapMutations(['对应函数名'])之后，直接this.对应函数名来调用
    changeExam (state, payload) {
        state.exam = payload.exam
    },
    changeExamItemVisible(state, payload) {
        state.itemVisible = {...state.itemVisible, ...payload}
    },
    changeRankForceUpdate (state, payload) {
        state.forceUpdate = payload.value
    },
    changeRankRemoveStar(state, payload){
        state.removeStar = payload.value
    },
    changeConcernedList(state, payload){
        state.concernedList = payload.value
    },
    changeExamProblems(state, payload) {
        state.examProblems = payload.examProblems;
        let tmp={};
        for(var j = 0,len = payload.examProblems.length; j < len; j++){
            tmp[payload.examProblems[j].displayId] = payload.examProblems[j].color;
        }
        state.disPlayIdMapColor = tmp;
    },
    changeExamRankLimit(state, payload) {
        state.rankLimit = payload.rankLimit
    },
    examIntoAccess(state, payload) {
        state.intoAccess = payload.intoAccess
    },
    changeGroupExamAuth(state, payload) {
        state.groupExamAuth = payload.groupExamAuth
    },
    examSubmitAccess(state, payload) {
        state.submitAccess = payload.submitAccess
    },
    clearExam (state) {
        state.exam = {}
        state.examProblems = []
        state.intoAccess = false
        state.submitAccess = false
        state.itemVisible = {
            table: true,
            chart: true,
            realName: false
        }
        state.forceUpdate = false
        state.removeStar = false
        state.groupExamAuth = 0
    },
    now(state, payload) {
        state.now = payload.now
    },
    nowAdd1s (state) {
        state.now = moment(state.now.add(1, 's'))
    },
}

const actions = {//专门用来处理mutation无法解决的异步任务，但仍需要通过mutation来间接实现
    getExam ({commit, rootState, dispatch}) {
        return new Promise((resolve, reject) => {
            api.getExam(rootState.route.params.examID).then((res) => {
                resolve(res)//执行成功,Promise状态变为fulfilled,则then可以执行，否则reject
                let exam = res.data.data
                commit('changeExam', {exam: exam})
                if (exam.gid) {
                    dispatch('getGroupExamAuth', {gid: exam.gid})
                    //this.$stroe.dispatch是 触发actions的方式
                    //同理再methods中使用...mapActions之后可以直接this.对应函数名调用
                }
                commit('now', {now: moment(exam.now)})
                if (exam.auth == EXAM_TYPE.PRIVATE) {
                    dispatch('getExamAccess',{auth:EXAM_TYPE.PRIVATE})
                }else if(exam.auth == EXAM_TYPE.PROTECTED){
                     dispatch('getExamAccess',{auth:EXAM_TYPE.PROTECTED})
                }
            }, err => {
                reject(err)
            })
        })
    },
    getScoreBoardExamInfo ({commit, rootState, dispatch}) {
        return new Promise((resolve, reject) => {
            api.getScoreBoardExamInfo(rootState.route.params.examID).then((res) => {
                resolve(res)
                let exam = res.data.data.exam;
                let problemList = res.data.data.problemList;
                commit('changeExam', {exam: exam})
                commit('changeExamProblems', {examProblems: problemList})
                commit('now', {now: moment(exam.now)})
            }, err => {
                reject(err)
            })
        })
    },

    getExamProblems ({commit, rootState}) {
        return new Promise((resolve, reject) => {
            api.getExamProblemList(rootState.route.params.examID).then(res => {
                resolve(res)
                commit('changeExamProblems', {examProblems: res.data.data})
            }, (err) => {
                commit('changeExamProblems', {examProblems: []})
                reject(err)
            })
        })
    },
    getExamAccess ({commit, rootState},examType) {
        return new Promise((resolve, reject) => {
            api.getExamAccess(rootState.route.params.examID).then(res => {
                if(examType.auth == EXAM_TYPE.PRIVATE){
                    commit('examIntoAccess', {intoAccess: res.data.data.access})
                }else{
                    commit('examSubmitAccess', {submitAccess: res.data.data.access})
                }
                resolve(res)
            }).catch()
        })
    },
    getGroupExamAuth ({commit, rootState}, gid) {
        return new Promise((resolve, reject) => {
            api.getGroupAuth(gid.gid).then(res => {
                commit('changeGroupExamAuth', {groupExamAuth: res.data.data})
                resolve(res)
            }).catch()
        })
    }
}

export default {
    state,
    mutations,
    getters,
    actions
}
