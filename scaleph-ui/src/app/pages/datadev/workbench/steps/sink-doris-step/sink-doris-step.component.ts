import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { DValidateRules, FormLayout } from 'ng-devui';
import { Dict } from 'src/app/@core/data/app.data';
import { DiJobStepAttr, STEP_ATTR_TYPE } from 'src/app/@core/data/datadev.data';
import { DataSourceService } from 'src/app/@core/services/datadev/datasource.service';
import { JobService } from 'src/app/@core/services/datadev/job.service';
import { CustomValidate } from 'src/app/@core/validate/CustomValidate';

@Component({
  selector: 'app-sink-doris-step',
  templateUrl: './sink-doris-step.component.html',
  styleUrls: ['../../workbench.component.scss'],
})
export class SinkDorisStepComponent implements OnInit {
  @Input() data;
  @Output() onSave = new EventEmitter<any>();
  jobId: string = '';
  stepCode: string = '';
  stepTitle: string = '';

  dataSourceType: string = 'Doris';
  dataSourceList: Dict[] = [];

  formLayout = FormLayout.Vertical;
  formConfig: { [Key: string]: DValidateRules } = {
    rule: { message: this.translate.instant('app.error.formValidateError'), messageShowType: 'text' },
    stepTitleRules: {
      validators: [{ required: true }],
    },
    dataSourceRules: {
      validators: [{ required: true }],
    },
    tableRules: {
      validators: [{ required: true }],
    },
  };
  formGroup: FormGroup = this.fb.group({
    stepTitle: [null],
    dataSource: [null],
    table: [null],
    batch_size: [null],
    interval: [null],
    max_retries: [null],
    doris_conf: [null],
  });

  constructor(
    private fb: FormBuilder,
    private translate: TranslateService,
    private dataSourceService: DataSourceService,
    private jobService: JobService
  ) {}

  ngOnInit(): void {
    this.jobId = this.data.jobId;
    this.stepCode = this.data.item?.id;
    this.stepTitle = this.data.item?.data.title;
    this.formGroup.patchValue({ stepTitle: this.stepTitle });
    this.dataSourceService.listByType(this.dataSourceType).subscribe((d) => {
      this.dataSourceList = d;
    });
    this.jobService.listStepAttr(this.jobId, this.stepCode).subscribe((d) => {
      let list: DiJobStepAttr[] = d;
      let stepAttrMap: Map<string, string> = new Map();
      for (const stepAttr of list) {
        stepAttrMap.set(stepAttr.stepAttrKey, stepAttr.stepAttrValue);
      }
      if (stepAttrMap.get(STEP_ATTR_TYPE.dataSource)) {
        this.formGroup.patchValue({ dataSource: JSON.parse(stepAttrMap.get(STEP_ATTR_TYPE.dataSource)) });
      }
      this.formGroup.patchValue({ table: stepAttrMap.get(STEP_ATTR_TYPE.table) });
      this.formGroup.patchValue({ batch_size: stepAttrMap.get(STEP_ATTR_TYPE.batchSize) });
      this.formGroup.patchValue({ interval: stepAttrMap.get(STEP_ATTR_TYPE.interval) });
      this.formGroup.patchValue({ max_retries: stepAttrMap.get(STEP_ATTR_TYPE.maxRetries) });
      this.formGroup.patchValue({ doris_conf: stepAttrMap.get(STEP_ATTR_TYPE.dorisConf) });
    });
  }
  submitForm() {
    CustomValidate.validateForm(this.formGroup);
    if (this.formGroup.valid) {
      let stepAttrMap: Map<string, string> = new Map();
      stepAttrMap.set(STEP_ATTR_TYPE.stepTitle, this.formGroup.get(STEP_ATTR_TYPE.stepTitle).value);
      stepAttrMap.set(STEP_ATTR_TYPE.dataSource, this.formGroup.get(STEP_ATTR_TYPE.dataSource).value);
      stepAttrMap.set(STEP_ATTR_TYPE.table, this.formGroup.get(STEP_ATTR_TYPE.table).value);
      stepAttrMap.set(STEP_ATTR_TYPE.batchSize, this.formGroup.get(STEP_ATTR_TYPE.batchSize).value);
      stepAttrMap.set(STEP_ATTR_TYPE.interval, this.formGroup.get(STEP_ATTR_TYPE.interval).value);
      stepAttrMap.set(STEP_ATTR_TYPE.maxRetries, this.formGroup.get(STEP_ATTR_TYPE.maxRetries).value);
      stepAttrMap.set(STEP_ATTR_TYPE.dorisConf, this.formGroup.get(STEP_ATTR_TYPE.dorisConf).value);
      this.onSave.emit(stepAttrMap);
    }
  }
}
