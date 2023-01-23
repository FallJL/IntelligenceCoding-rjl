<template>
  <div class="view">
    <el-card>
      <div slot="header">
        <span class="panel-title home-title">
          {{ title }}
        </span>
      </div>
      <el-form label-position="top"><!--表单 顶部对齐-->
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item :label="$t('m.Exam_Title')" required>
              <el-input
                  v-model="exam.title"
                  :placeholder="$t('m.Exam_Title')"
              ></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item :label="$t('m.Exam_Description')" required>
              <Editor :value.sync="exam.description"></Editor>
            </el-form-item>
          </el-col>
          <el-col :md="8" :xs="24">
            <el-form-item :label="$t('m.Exam_Start_Time')" required>
              <el-date-picker
                  v-model="exam.startTime"
                  @change="changeDuration"
                  type="datetime"
                  :placeholder="$t('m.Exam_Start_Time')"
              >
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :md="8" :xs="24">
            <el-form-item :label="$t('m.Exam_End_Time')" required>
              <el-date-picker
                  v-model="exam.endTime"
                  @change="changeDuration"
                  type="datetime"
                  :placeholder="$t('m.Exam_End_Time')"
              >
              </el-date-picker>
            </el-form-item>
          </el-col>

          <el-col :md="8" :xs="24">
            <el-form-item :label="$t('m.Exam_Duration')" required>
              <el-input v-model="durationText" disabled></el-input>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row>
          <el-col :md="8" :xs="24">
            <el-form-item :label="$t('m.Exam_Rule_Type')" required>
              <el-radio
                  class="radio"
                  v-model="exam.type"
                  :label="0"
                  @change="setSealRankTimeDefaultValue"
                  :disabled="disableRuleType"
              >ACM
              </el-radio
              >
              <el-radio
                  class="radio"
                  v-model="exam.type"
                  :label="1"
                  :disabled="disableRuleType"
                  @change="setSealRankTimeDefaultValue"
              >OI
              </el-radio
              >
            </el-form-item>
          </el-col>

          <el-col :md="8" :xs="24">
            <el-form-item
                :label="$t('m.OI_Rank_Score_Type')"
                v-show="exam.type == 1"
            >
              <el-radio
                  class="radio"
                  v-model="exam.oiRankScoreType"
                  label="Recent"
              >{{ $t('m.OI_Rank_Score_Type_Recent') }}
              </el-radio
              >
              <el-radio
                  class="radio"
                  v-model="exam.oiRankScoreType"
                  label="Highest"
              >{{ $t('m.OI_Rank_Score_Type_Highest') }}
              </el-radio
              >
            </el-form-item>
          </el-col>
        </el-row>

        <el-row>
          <el-col :md="8" :xs="24" v-if="exam.sealRank">
            <el-form-item :label="$t('m.Seal_Time_Rank')" required>
              <el-switch
                  v-model="exam.sealRank"
                  active-color="#13ce66"
                  inactive-color="#ff4949"
              >
              </el-switch>
            </el-form-item>
          </el-col>

          <el-col :md="24" :xs="24" v-else>
            <el-form-item :label="$t('m.Real_Time_Rank')" required>
              <el-switch
                  v-model="exam.sealRank"
                  active-color="#13ce66"
                  inactive-color=""
                  @change="setSealRankTimeDefaultValue"
              >
              </el-switch>
            </el-form-item>
          </el-col>

          <el-col :md="8" :xs="24">
            <el-form-item
                :label="$t('m.Seal_Rank_Time')"
                :required="exam.sealRank"
                v-show="exam.sealRank"
            >
              <el-select v-model="seal_rank_time">
                <el-option
                    :label="$t('m.Exam_Seal_Half_Hour')"
                    :value="0"
                    :disabled="exam.duration < 1800"
                ></el-option>
                <el-option
                    :label="$t('m.Exam_Seal_An_Hour')"
                    :value="1"
                    :disabled="exam.duration < 3600"
                ></el-option>
                <el-option
                    :label="$t('m.Exam_Seal_All_Hour')"
                    :value="2"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :md="8" :xs="24">
            <el-form-item
                :label="$t('m.Auto_Real_Rank')"
                required
                v-show="exam.sealRank"
            >
              <el-switch
                  v-model="exam.autoRealRank"
                  :active-text="$t('m.Real_Rank_After_Exam')"
                  :inactive-text="$t('m.Seal_Rank_After_Exam')"
              >
              </el-switch>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row>
          <el-col :md="8" :xs="24">
            <el-form-item :label="$t('m.Exam_Outside_ScoreBoard')" required>
              <el-switch
                  v-model="exam.openRank"
                  :active-text="$t('m.Open')"
                  :inactive-text="$t('m.Close')"
              >
              </el-switch>
            </el-form-item>
          </el-col>

          <el-col :md="8" :xs="24">
            <el-form-item :label="$t('m.Print_Func')" required>
              <el-switch
                  v-model="exam.openPrint"
                  :active-text="$t('m.Support_Offline_Print')"
                  :inactive-text="$t('m.Not_Support_Print')"
              >
              </el-switch>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row>
          <el-col :span="24">
            <el-form-item :label="$t('m.Rank_Show_Name')" required>
              <el-radio-group v-model="exam.rankShowName">
                <el-radio label="username">{{
                    $t('m.Show_Username')
                  }}
                </el-radio>
                <el-radio label="nickname">{{
                    $t('m.Show_Nickname')
                  }}
                </el-radio>
                <el-radio label="realname">{{
                    $t('m.Show_Realname')
                  }}
                </el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>

          <el-col>
            <el-form-item :label="$t('m.Star_User_UserName')" required>
              <el-tag
                  v-for="username in exam.starAccount"
                  closable
                  :close-transition="false"
                  :key="username"
                  type="warning"
                  size="medium"
                  @close="removeStarUser(username)"
                  style="margin-right: 7px;margin-top:4px"
              >{{ username }}
              </el-tag
              >
              <el-input
                  v-if="inputVisible"
                  size="medium"
                  class="input-new-star-user"
                  v-model="starUserInput"
                  :trigger-on-focus="true"
                  @keyup.enter.native="addStarUser"
                  @blur="addStarUser"
              >
              </el-input>
              <el-tooltip
                  effect="dark"
                  :content="$t('m.Add')"
                  placement="top"
                  v-else
              >
                <el-button
                    class="button-new-tag"
                    size="small"
                    @click="inputVisible = true"
                    icon="el-icon-plus"
                ></el-button>
              </el-tooltip>
            </el-form-item>
          </el-col>

          <el-col :md="8" :xs="24">
            <el-form-item :label="$t('m.Exam_Auth')" required>
              <el-select v-model="exam.auth">
                <el-option :label="$t('m.Public')" :value="0"></el-option>
                <el-option :label="$t('m.Private')" :value="1"></el-option>
                <el-option :label="$t('m.Protected')" :value="2"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :md="8" :xs="24">
            <el-form-item
                :label="$t('m.Exam_Password')"
                v-show="exam.auth != 0"
                :required="exam.auth != 0"
            >
              <el-input
                  v-model="exam.pwd"
                  :placeholder="$t('m.Exam_Password')"
              ></el-input>
            </el-form-item>
          </el-col>
          <el-col :md="8" :xs="24">
            <el-form-item
                :label="$t('m.Account_Limit')"
                v-show="exam.auth != 0"
                :required="exam.auth != 0"
            >
              <el-switch v-model="exam.openAccountLimit"></el-switch>
            </el-form-item>
          </el-col>

          <template v-if="exam.openAccountLimit">
            <el-form :model="formRule">
              <el-col :md="6" :xs="24">
                <el-form-item :label="$t('m.Prefix')" prop="prefix">
                  <el-input
                      v-model="formRule.prefix"
                      placeholder="Prefix"
                  ></el-input>
                </el-form-item>
              </el-col>
              <el-col :md="6" :xs="24">
                <el-form-item :label="$t('m.Suffix')" prop="suffix">
                  <el-input
                      v-model="formRule.suffix"
                      placeholder="Suffix"
                  ></el-input>
                </el-form-item>
              </el-col>
              <el-col :md="6" :xs="24">
                <el-form-item :label="$t('m.Start_Number')" prop="number_from">
                  <el-input-number
                      v-model="formRule.number_from"
                      style="width: 100%"
                  ></el-input-number>
                </el-form-item>
              </el-col>
              <el-col :md="6" :xs="24">
                <el-form-item :label="$t('m.End_Number')" prop="number_to">
                  <el-input-number
                      v-model="formRule.number_to"
                      style="width: 100%"
                  ></el-input-number>
                </el-form-item>
              </el-col>

              <div
                  class="userPreview"
                  v-if="formRule.number_from <= formRule.number_to"
              >
                {{ $t('m.The_allowed_account_will_be') }}
                {{ formRule.prefix + formRule.number_from + formRule.suffix }},
                <span v-if="formRule.number_from + 1 < formRule.number_to">
                  {{
                    formRule.prefix +
                    (formRule.number_from + 1) +
                    formRule.suffix +
                    '...'
                  }}
                </span>
                <span v-if="formRule.number_from + 1 <= formRule.number_to">
                  {{ formRule.prefix + formRule.number_to + formRule.suffix }}
                </span>
              </div>

              <el-col :md="24" :xs="24">
                <el-form-item :label="$t('m.Extra_Account')" prop="prefix">
                  <el-input
                      type="textarea"
                      :placeholder="$t('m.Extra_Account_Tips')"
                      :rows="8"
                      v-model="formRule.extra_account"
                  >
                  </el-input>
                </el-form-item>
              </el-col>
            </el-form>
          </template>
        </el-row>

        <el-row>
          <el-col :span="24">
            <el-card>

              <!--  条件表格 -->
              <div>
                <el-table
                    :data="dataList"
                    style="width: 100%"
                    highlight-current-row>
                  <el-table-column label="序号" width="55" type="index"></el-table-column>
                  <el-table-column label="算法分类" width="280">
                    <template slot-scope="scope">
                      <el-select v-model="scope.row.算法分类" placeholder="请选择"
                                 filterable clearable @change="nameChange(scope.row.算法分类, scope.$index)">
                        <el-option v-for="item in classifications"
                                   :label="item.key" :value="item.key">
                        </el-option>
                      </el-select>
                    </template>
                  </el-table-column>
                  <el-table-column label="算法标签" width="170">
                    <template slot-scope="scope">
                      <el-select v-model="scope.row.算法标签" placeholder="请选择" filterable clearable>
                        <el-option
                            v-for="item in scope.row.AlgorithmLabel"
                            :label="item" :value="item">
                        </el-option>
                      </el-select>
                    </template>
                  </el-table-column>
                  <el-table-column label="难度" width="250">
                    <template slot-scope="scope">
                      <el-select v-model="scope.row.难度" placeholder="请选择"
                                 filterable clearable>
                        <el-option v-for="item in difficulties"
                                   :label="item.key" :value="item.key">
                        </el-option>
                      </el-select>
                    </template>
                  </el-table-column>
                  <el-table-column label="操作">
                    <template slot-scope="scope">
                      <el-button size="mini" type="danger"
                                 @click="handleDataDelete(scope.$index, scope.row)">删除
                      </el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
              <div slot="header">
                <div style="display: inline;">
                  <el-button type="info" @click="resetDataList">清空</el-button>
                  <el-button type="warning" @click="handleDataAdd">添加题目</el-button>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </el-form>

      <el-button type="primary" @click.native="saveExam">{{
          $t('m.Save')
        }}
      </el-button>
    </el-card>
  </div>
</template>

<script>
import api from '@/common/api';
import time from '@/common/time';
import moment from 'moment';
import {mapGetters} from 'vuex';
import myMessage from '@/common/message';

const Editor = () => import('@/components/admin/Editor.vue');
export default {
  name: 'CreateExam',
  components: {
    Editor,
  },
  data() {
    return {
      title: 'Create Exam',
      disableRuleType: false,
      durationText: '', // 比赛时长文本表示
      seal_rank_time: 2, // 当开启封榜模式，即实时榜单关闭时，可选择前半小时，前一小时，全程封榜,默认全程封榜
      exam: {
        title: '',
        description: '',
        startTime: '',
        endTime: '',
        duration: 0,
        type: 0,
        pwd: '',
        sealRank: false,
        sealRankTime: '', //封榜时间
        autoRealRank: true,
        auth: 0,
        openPrint: false,
        rankShowName: 'username',
        openAccountLimit: false,
        accountLimitRule: '',
        starAccount: [],
        oiRankScoreType: 'Recent',
      },
      formRule: {
        prefix: '',
        suffix: '',
        number_from: 0,
        number_to: 10,
        extra_account: '',
      },
      starUserInput: '',
      inputVisible: false,
      // 添加题目
      classifications: [
        {key: '基础算法', conditionType: 'condition1'},
        {key: '数据结构', conditionType: 'condition2'},
        {key: '图论', conditionType: 'condition3'},
        {key: '动态规划', conditionType: 'condition4'},
        {key: '数学', conditionType: 'condition5'},
      ],
      //不同的活动类型的值
      conditions_options: {
        'condition1': ['复杂度分析', '枚举', '模拟', '递归', '分治', '贪心', '排序', '二进制', '前缀和', '差分', '二分', '三分', '倍增', '离散化', '构造', 'Hash'],
        'condition2': ['栈', '队列', '链表', '哈希表', '并查集', '可并堆', 'set', 'bitset'],
        'condition3': ['树上问题','有向无环图','拓扑排序','最小生成树','最短路','连通性','网络流','图的匹配'],
        'condition4': ['记忆化搜索', '背包DP', '树形DP','状压DP','数位DP','区间DP'],
        'condition5': ['高精度计算', '快速幂', '位运算','素数']
      },
      difficulties: [
        {key: '简单'},
        {key: '中等'},
        {key: '困难'},
      ],
      //数据列表
      dataList: [{
        算法分类: '',
        算法标签: '',
        难度: '',
        AlgorithmLabel: [],
        //对应的值下拉框的数据
        difficulties: []
      }],
    }
  }
  ,
  mounted() {
    if (this.$route.name === 'admin-edit-exam') {
      this.title = this.$i18n.t('m.Edit_Exam');
      this.disableRuleType = true;
      this.getExamByEid();
    } else {
      this.title = this.$i18n.t('m.Create_Exam');
      this.disableRuleType = false;
    }
  }
  ,
  watch: {
    $route() {
      if (this.$route.name === 'admin-edit-exam') {
        this.title = this.$i18n.t('m.Edit_Exam');
        this.disableRuleType = true;
        this.getExamByEid();
      } else {
        this.title = this.$i18n.t('m.Create_Exam');
        this.disableRuleType = false;
        this.exam = {};
      }
    }
    ,
  }
  ,
  computed: {
    ...
        mapGetters(['userInfo']),
  }
  ,
  methods: {
    getExamByEid() {
      api
          .admin_getExam(this.$route.params.examId)
          .then((res) => {
            let data = res.data.data;
            this.exam = data;
            this.changeDuration();
            // 封榜时间转换
            let halfHour = moment(this.exam.endTime)
                .subtract(1800, 'seconds')
                .toString();
            let oneHour = moment(this.exam.endTime)
                .subtract(3600, 'seconds')
                .toString();
            let allHour = moment(this.exam.startTime).toString();
            let sealRankTime = moment(this.exam.sealRankTime).toString();
            switch (sealRankTime) {
              case halfHour:
                this.seal_rank_time = 0;
                break;
              case oneHour:
                this.seal_rank_time = 1;
                break;
              case allHour:
                this.seal_rank_time = 2;
                break;
            }
            if (this.exam.accountLimitRule) {
              this.formRule = this.changeStrToAccountRule(
                  this.exam.accountLimitRule
              );
            }
          })
          .catch(() => {
          });
    }
    ,

    saveExam() {
      if (!this.exam.title) {
        myMessage.error(
            this.$i18n.t('m.Exam_Title') + ' ' + this.$i18n.t('m.is_required')
        );
        return;
      }
      if (!this.exam.description) {
        myMessage.error(
            this.$i18n.t('m.Exam_Description') +
            ' ' +
            this.$i18n.t('m.is_required')
        );
        return;
      }
      if (!this.exam.startTime) {
        myMessage.error(
            this.$i18n.t('m.Exam_Start_Time') +
            ' ' +
            this.$i18n.t('m.is_required')
        );
        return;
      }
      if (!this.exam.endTime) {
        myMessage.error(
            this.$i18n.t('m.Exam_End_Time') +
            ' ' +
            this.$i18n.t('m.is_required')
        );
        return;
      }
      if (!this.exam.duration || this.exam.duration <= 0) {
        myMessage.error(this.$i18n.t('m.Exam_Duration_Check'));
        return;
      }
      if (this.exam.auth != 0 && !this.exam.pwd) {
        myMessage.error(
            this.$i18n.t('m.Exam_Password') +
            ' ' +
            this.$i18n.t('m.is_required')
        );
        return;
      }

      if (this.exam.openAccountLimit) {
        this.exam.accountLimitRule = this.changeAccountRuleToStr(
            this.formRule
        );
      }

      let funcName =
          this.$route.name === 'admin-edit-exam'
              ? 'admin_editExam'
              : 'admin_createExam';

      switch (this.seal_rank_time) {
        case 0: // 结束前半小时
          this.exam.sealRankTime = moment(this.exam.endTime).subtract(
              1800,
              'seconds'
          );
          break;
        case 1: // 结束前一小时
          this.exam.sealRankTime = moment(this.exam.endTime).subtract(
              3600,
              'seconds'
          );
          break;
        case 2: // 全程
          this.exam.sealRankTime = moment(this.exam.startTime);
      }
      let data = Object.assign({}, this.exam);
      if (funcName === 'admin_createExam') {
        data['uid'] = this.userInfo.uid;
        data['author'] = this.userInfo.username;
      }

      api[funcName](data)
          .then((res) => {
            myMessage.success('success');
            this.$router.push({
              name: 'admin-exam-list',
              query: {refresh: 'true'},
            });
          })
          .catch(() => {
          });
    }
    ,
    changeDuration() {
      let start = this.exam.startTime;
      let end = this.exam.endTime;
      let durationMS = time.durationMs(start, end);
      if (durationMS < 0) {
        this.durationText = this.$i18n.t('m.Exam_Time_Check');
        this.exam.duration = 0;
        return;
      }
      if (start != '' && end != '') {
        this.durationText = time.formatSpecificDuration(start, end);
        this.exam.duration = durationMS;
      }
    }
    ,
    changeAccountRuleToStr(formRule) {
      let result =
          '<prefix>' +
          formRule.prefix +
          '</prefix><suffix>' +
          formRule.suffix +
          '</suffix><start>' +
          formRule.number_from +
          '</start><end>' +
          formRule.number_to +
          '</end><extra>' +
          formRule.extra_account +
          '</extra>';
      return result;
    }
    ,
    changeStrToAccountRule(value) {
      let reg =
          '<prefix>([\\s\\S]*?)</prefix><suffix>([\\s\\S]*?)</suffix><start>([\\s\\S]*?)</start><end>([\\s\\S]*?)</end><extra>([\\s\\S]*?)</extra>';
      let re = RegExp(reg, 'g');
      let tmp = re.exec(value);
      return {
        prefix: tmp[1],
        suffix: tmp[2],
        number_from: tmp[3],
        number_to: tmp[4],
        extra_account: tmp[5],
      };
    }
    ,

    addStarUser() {
      this.starUserInput = this.starUserInput.replace(/(^\s*)|(\s*$)/g, '');
      if (this.starUserInput) {
        for (var i = 0; i < this.exam.starAccount.length; i++) {
          if (this.exam.starAccount[i] == this.starUserInput) {
            myMessage.warning(this.$i18n.t('m.Add_Star_User_Error'));
            this.starUserInput = '';
            return;
          }
        }
        this.exam.starAccount.push(this.starUserInput);
        this.inputVisible = false;
        this.starUserInput = '';
      }
    }
    ,

    // 根据UserName 从打星用户列表中移除
    removeStarUser(username) {
      this.exam.starAccount.splice(
          this.exam.starAccount.map((item) => item.name).indexOf(username),
          1
      );
    }
    ,

    setSealRankTimeDefaultValue() {
      if (this.exam.sealRank == true) {
        if (this.exam.type == 0) {
          // ACM比赛开启封榜 默认为一小时,如果比赛时长小于一小时，则默认为全程
          if (this.exam.duration < 3600) {
            this.seal_rank_time = 2;
          } else {
            this.seal_rank_time = 1;
          }
        } else {
          // OI比赛开启封榜 默认全程
          this.seal_rank_time = 2;
        }
      }
    }
    ,
    resetDataList() {
      this.dataList = [{
        算法分类: '',
        算法标签: '',
        难度: '',
        AlgorithmLabel: [],
        difficulties: []
      }];
      this.sortName = '';
      this.sortValue = '';
    },
//删除一行
    handleDataDelete(index, row) {
      this.dataList.splice(index, 1);
    },
//插入一行
    handleDataAdd() {
      if (this.dataList == undefined) {
        this.dataList = [];
      }
      let obj = {};
      obj.算法分类 = '';
      obj.算法标签 = '';
      obj.难度 = '';
      obj.AlgorithmLabel = [];
      obj.difficulties = [];
      this.dataList.push(obj);
    },
//分类发生变化，相应的标签跟着发生变化
    nameChange(fieldName, index) {
      this.dataList[index].AlgorithmLabel = [];
      this.dataList[index].difficulties = [];
      this.getConditionAndValue(fieldName, index);
      this.dataList[index].算法标签 = '';
      this.dataList[index].难度 = '';
    },
    getConditionAndValue(fieldName, index) {
      for (var i = 0; i < this.classifications.length; i++) {
        if (this.classifications[i].key == fieldName) {
          //赋值活动类型
          this.dataList[index].AlgorithmLabel = this.conditions_options[this.classifications[i].conditionType];
          break;
        }
      }
    },
  }
}
;
</script>
<style scoped>
.userPreview {
  padding-left: 10px;
  padding-top: 20px;
  padding-bottom: 20px;
  color: red;
  font-size: 16px;
  margin-bottom: 10px;
}

.input-new-star-user {
  width: 200px;
}
</style>
