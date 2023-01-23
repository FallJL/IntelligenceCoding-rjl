import moment from 'moment'
import api from '@/common/api'
import { PAPER_STATUS, PAPER_TYPE } from '@/common/constants'
import time from '@/common/time'

const state = { //state存放全局共享的数据
    now: moment(),
    intoAccess: false, // 考试进入权限
    submitAccess:false, // 保护考试的提交权限
    forceUpdate: false, // 强制实时榜单
    removeStar: false, // 榜单去除打星队伍
    concernedList:[], // 关注队伍
    paper: {
        auth: PAPER_TYPE.PUBLIC,
        openPrint: false,
        rankShowName:'username',
    },
    paperProblems: [],
    itemVisible: {
        table: true,
        chart: true,
    },
    disPlayIdMapColor:{}, // 展示id对应的气球颜色
    groupPaperAuth: 0,
}

const getters = {//通过对state中数据进行加工处理形成新的数据，类似于vue中的计算属性，store中数据改变，getter随之改变
    //(1)通过this.$store.getter.对应函数名直接调用
    //(2)...mapGetter(['对应函数名'])
    paperStatus: (state, getters) => {
        return state.paper.status;
    },
    paperRuleType: (state,getters) => {
        return state.paper.type;
    },
    isPaperAdmin: (state, getters, _, rootGetters) => {
        return rootGetters.isAuthenticated &&
            (state.paper.author === rootGetters.userInfo.username || rootGetters.isSuperAdmin || state.groupPaperAuth == 5)
    },
    canSubmit:(state, getters)=>{
        return state.intoAccess||state.submitAccess || state.paper.auth === PAPER_TYPE.PUBLIC ||getters.isPaperAdmin
    },
    paperMenuDisabled: (state, getters) => {
        // 考试创建者或者超级管理员可以直接查看
        if (getters.isPaperAdmin) return false
        // 未开始不可查看
        if(getters.paperStatus === PAPER_STATUS.SCHEDULED) return true

        if (state.paper.auth === PAPER_TYPE.PRIVATE) {
            // 私有赛需要通过验证密码方可查看考试
            return !state.intoAccess
        }

    },

    // 榜单是否实时刷新
    PaperRealTimePermission: (state, getters, _, rootGetters) => {
        // 考试若是已结束，便是最后榜单
        if (getters.paperStatus === PAPER_STATUS.ENDED) {
            return true
        }
        // 考试管理员直接可看到实时榜单
        if(getters.isPaperAdmin){
            return true
        }
        // 考试是否开启
        if(state.paper.sealRank === true){
            // 当前时间在封榜时间之后，即不刷新榜单
            return !state.now.isAfter(moment(state.paper.sealRankTime))
        }else{
            return true
        }
    },
    problemSubmitDisabled: (state, getters, _, rootGetters) => {
        // 考试结束不可交题
        if (getters.paperStatus === PAPER_STATUS.ENDED) {
            return true

            // 考试未开始不可交题，除非是考试管理者
        } else if (getters.paperStatus === PAPER_STATUS.SCHEDULED) {
            return !getters.isPaperAdmin
        }
        // 未登录不可交题
        return !rootGetters.isAuthenticated
    },
    // 是否需要显示密码验证框
    passwordFormVisible: (state, getters) => {
        // 如果是公开赛，保护赛，或已注册过，管理员都不用再显示
        return state.paper.auth !== PAPER_TYPE.PUBLIC &&state.paper.auth !== PAPER_TYPE.PROTECTED &&!state.intoAccess && !getters.isPaperAdmin
    },
    paperStartTime: (state) => {
        return moment(state.paper.startTime)
    },
    paperEndTime: (state) => {
        return moment(state.paper.endTime)
    },
    // 考试计时文本显示
    countdown: (state, getters) => {
        // 还未开始的显示
        if (getters.paperStatus === PAPER_STATUS.SCHEDULED) {

            let durationMs = getters.paperStartTime.diff(state.now, 'seconds')

            let duration = moment.duration(durationMs, 'seconds')
            // time is too long
            if (duration.weeks() > 0) {
                return 'Start At ' + duration.humanize()
            }

            if(duration.asSeconds()<=0){
                state.paper.status = PAPER_STATUS.RUNNING
            }

            let texts = time.secondFormat(durationMs)
            return '-' + texts
            // 考试进行中的显示
        } else if (getters.paperStatus === PAPER_STATUS.RUNNING) {
            // 倒计时文本显示
            if(getters.paperEndTime.diff(state.now, 'seconds')>0){
                let texts = time.secondFormat(getters.paperEndTime.diff(state.now, 'seconds'))
                return '-' + texts
            }else{
                state.paper.status = PAPER_STATUS.ENDED
                return "00:00:00"
            }

        } else {
            return 'Ended'
        }
    },
    // 考试开始到现在经过的秒数
    BeginToNowDuration:(state,getters)=>{
        return moment.duration(state.now.diff(getters.paperStartTime, 'seconds'), 'seconds').asSeconds()
    },

    // 考试进度条显示
    progressValue:(state,getters)=>{
        // 还未开始的显示
        if (getters.paperStatus === PAPER_STATUS.SCHEDULED) {
            return 0;
            // 考试进行中的显示
        } else if (getters.paperStatus === PAPER_STATUS.RUNNING) {
            // 获取考试开始到现在经过的秒数
            let duration = getters.BeginToNowDuration
            // 消耗时间除以整体时间
            return (duration / state.paper.duration)*100
        }else{
            return 100;
        }
    },
}

const mutations = {//用于变更store中的数据，不允许异步代码
    //（1）可以在methods中通过this.$store.commit('对应函数名')来调用对应函数
    //（2）也可以...mapMutations(['对应函数名'])之后，直接this.对应函数名来调用
    changePaper (state, payload) {
        state.paper = payload.paper
    },
    changePaperItemVisible(state, payload) {
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
    changePaperProblems(state, payload) {
        state.paperProblems = payload.paperProblems;
        let tmp={};
        for(var j = 0,len = payload.paperProblems.length; j < len; j++){
            tmp[payload.paperProblems[j].displayId] = payload.paperProblems[j].color;
        }
        state.disPlayIdMapColor = tmp;
    },
    changePaperRankLimit(state, payload) {
        state.rankLimit = payload.rankLimit
    },
    paperIntoAccess(state, payload) {
        state.intoAccess = payload.intoAccess
    },
    changeGroupPaperAuth(state, payload) {
        state.groupPaperAuth = payload.groupPaperAuth
    },
    paperSubmitAccess(state, payload) {
        state.submitAccess = payload.submitAccess
    },
    clearPaper (state) {
        state.paper = {}
        state.paperProblems = []
        state.intoAccess = false
        state.submitAccess = false
        state.itemVisible = {
            table: true,
            chart: true,
            realName: false
        }
        state.forceUpdate = false
        state.removeStar = false
        state.groupPaperAuth = 0
    },
    now(state, payload) {
        state.now = payload.now
    },
    nowAdd1s (state) {
        state.now = moment(state.now.add(1, 's'))
    },
}

const actions = {//专门用来处理mutation无法解决的异步任务，但仍需要通过mutation来间接实现
    getPaper ({commit, rootState, dispatch}) {
        return new Promise((resolve, reject) => {
            api.getPaper(rootState.route.params.paperID).then((res) => {
                resolve(res)//执行成功,Promise状态变为fulfilled,则then可以执行，否则reject
                let paper = res.data.data
                commit('changePaper', {paper: paper})
                if (paper.gid) {
                    dispatch('getGroupPaperAuth', {gid: paper.gid})
                    //this.$stroe.dispatch是 触发actions的方式
                    //同理再methods中使用...mapActions之后可以直接this.对应函数名调用
                }
                commit('now', {now: moment(paper.now)})
                if (paper.auth == PAPER_TYPE.PRIVATE) {
                    dispatch('getPaperAccess',{auth:PAPER_TYPE.PRIVATE})
                }else if(paper.auth == PAPER_TYPE.PROTECTED){
                    dispatch('getPaperAccess',{auth:PAPER_TYPE.PROTECTED})
                }
            }, err => {
                reject(err)
            })
        })
    },
    getScoreBoardPaperInfo ({commit, rootState, dispatch}) {
        return new Promise((resolve, reject) => {
            api.getScoreBoardPaperInfo(rootState.route.params.paperID).then((res) => {
                resolve(res)
                let paper = res.data.data.paper;
                let problemList = res.data.data.problemList;
                commit('changePaper', {paper: paper})
                commit('changePaperProblems', {paperProblems: problemList})
                commit('now', {now: moment(paper.now)})
            }, err => {
                reject(err)
            })
        })
    },

    getPaperProblems ({commit, rootState}) {
        return new Promise((resolve, reject) => {
            api.getPaperProblemList(rootState.route.params.paperID).then(res => {
                resolve(res)
                commit('changePaperProblems', {paperProblems: res.data.data})
            }, (err) => {
                commit('changePaperProblems', {paperProblems: []})
                reject(err)
            })
        })
    },
    getPaperAccess ({commit, rootState},paperType) {
        return new Promise((resolve, reject) => {
            api.getPaperAccess(rootState.route.params.paperID).then(res => {
                if(paperType.auth == PAPER_TYPE.PRIVATE){
                    commit('paperIntoAccess', {intoAccess: res.data.data.access})
                }else{
                    commit('paperSubmitAccess', {submitAccess: res.data.data.access})
                }
                resolve(res)
            }).catch()
        })
    },
    getGroupPaperAuth ({commit, rootState}, gid) {
        return new Promise((resolve, reject) => {
            api.getGroupAuth(gid.gid).then(res => {
                commit('changeGroupPaperAuth', {groupPaperAuth: res.data.data})
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
