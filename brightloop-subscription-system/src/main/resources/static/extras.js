(function(){
const H=s=>s==null?'':String(s).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const M=v=>'₹ '+Number(v||0).toLocaleString('en-IN',{maximumFractionDigits:2});
const J=(m,b)=>({method:m,body:JSON.stringify(b||{})});
const $=id=>document.getElementById(id),V=id=>$(id).value;
const tbl=(h,r)=>`<div class="panel"><div class="tablewrap"><table><thead><tr>${h.map(x=>`<th>${x}</th>`).join('')}</tr></thead><tbody>${r.length?r.map(x=>`<tr>${x.map(c=>`<td>${c}</td>`).join('')}</tr>`).join(''):`<tr><td colspan="${h.length}">Nothing yet</td></tr>`}</tbody></table></div></div>`;
const B=(t,f)=>`<button class="btn" style="padding:4px 10px;margin:2px" onclick="${f}">${t}</button>`;
const IN=(id,ph,t='text',v='')=>`<input id="${id}" type="${t}" placeholder="${ph}" title="${ph}" value="${v}" style="padding:8px;margin:3px;border:1px solid #cbd5e1;border-radius:8px">`;
const SE=(id,o)=>`<select id="${id}" style="padding:8px;margin:3px;border:1px solid #cbd5e1;border-radius:8px">${o.map(x=>`<option value="${x[0]}">${H(x[1])}</option>`).join('')}</select>`;
const card=h=>`<div class="panel" style="padding:14px;margin-bottom:14px">${h}</div>`;
let cur=null,D={};const X=window.XA={};
async function go(fn,msg){try{await fn();if(msg!=='')toast(msg||'Done')}catch(e){toast(e.message)}if(cur)cur.render()}
const staff=()=>currentUser.role!=='CUSTOMER',admin=()=>currentUser.role==='ADMIN';
const badge=s=>`<b>${H(s)}</b>`;
// ---- Quotations ----
async function rQuotes(){const el=$('quotes_body');
 if(!staff()){const s=(await req('/customer/subscriptions')).filter(x=>['QUOTATION','CONFIRMED'].includes(x.status));
  el.innerHTML=tbl(['No.','Plan','Qty','Amount (excl. tax)','Valid until','Status',''],s.map(x=>[H(x.number),H(x.plan.name),x.quantity,M(x.unitPrice*x.quantity-(x.discountAmount||0)),H(x.validUntil||'-'),badge(x.status),x.status==='QUOTATION'?B('Accept',`XA.cq(${x.id},'accept')`)+B('Reject',`XA.cq(${x.id},'reject')`):'']));return}
 const [subs,tp,cu,tx,pl]=await Promise.all([req('/subscriptions'),req('/templates'),req('/customers'),req('/taxes'),req('/plans')]);D.plans=pl;
 let h=card(`<b>New quotation from template</b><br>${SE('q_tpl',tp.map(t=>[t.id,`${t.name} (${t.plan.name} x${t.quantity}, ${t.validDays}d)`]))}${SE('q_cu',cu.map(c=>[c.id,c.name]))}${IN('q_start','Start date','date')}${B('Create quotation','XA.qnew()')}`);
 if(admin())h+=card(`<b>New template (admin)</b><br>${IN('t_name','Template name')}${SE('t_plan',(D.plans||[]).map(p=>[p.id,p.name]))}${IN('t_qty','Quantity','number','1')}${IN('t_days','Valid days','number','30')}${SE('t_tax',[['','Default tax 18%'],...tx.filter(t=>t.active).map(t=>[t.id,`${t.name} (${t.ratePercent}%)`])])}${B('Save template','XA.tnew()')}`);
 const rows=subs.filter(x=>['DRAFT','QUOTATION','CONFIRMED'].includes(x.status)).map(x=>[H(x.number),H(x.customer.name),H(x.plan.name),x.quantity,H(x.validUntil||'-'),badge(x.status),
  (x.status==='DRAFT'?B('Send quotation',`XA.qsend(${x.id})`):'')+(x.status==='QUOTATION'?B('Mark accepted',`XA.qst(${x.id},'CONFIRMED')`):'')+(x.status==='CONFIRMED'?B('Activate subscription',`XA.qst(${x.id},'ACTIVE')`):'')+(x.status!=='DRAFT'?B('Cancel',`XA.qst(${x.id},'CLOSED')`):'')]);
 el.innerHTML=h+tbl(['No.','Customer','Plan','Qty','Valid until','Status',''],rows)}
X.cq=(id,a)=>go(()=>req(`/customer/quotations/${id}/${a}`,J('POST')),a==='accept'?'Quotation accepted':'Quotation rejected');
X.qnew=()=>{if(!V('q_tpl')||!V('q_cu'))return toast('Create a template and choose a customer first');go(()=>req('/quotations/from-template',J('POST',{templateId:V('q_tpl'),customerId:V('q_cu'),startDate:V('q_start')})),'Draft quotation created')};
X.tnew=()=>{if(!V('t_name').trim()||!V('t_plan'))return toast('Enter a template name and choose a plan');go(()=>req('/templates',J('POST',{name:V('t_name'),planId:V('t_plan'),quantity:V('t_qty'),validDays:V('t_days'),taxRuleId:V('t_tax')})),'Template saved')};
X.qsend=id=>go(()=>req(`/quotations/${id}/send`,J('POST')),'Quotation sent to customer');
X.qst=(id,s)=>go(()=>req(`/subscriptions/${id}/status`,J('PUT',{status:s})),'Status updated');
// ---- Tax rules ----
async function rTax(){const t=await req('/taxes');
 $('tax_body').innerHTML=(admin()?card(`<b>New tax rule</b><br>${IN('x_name','Tax name')}${IN('x_rate','Rate %','number')}${B('Add','XA.xnew()')}`):'')+tbl(['Name','Rate','Status',admin()?'':''],t.map(x=>[H(x.name),x.ratePercent+'%',x.active?'Active':'Inactive',admin()?B('Edit rate',`XA.xedit(${x.id},${x.ratePercent})`)+B(x.active?'Deactivate':'Activate',`XA.xtog(${x.id},${!x.active})`):'']))}
X.xnew=()=>go(()=>req('/taxes',J('POST',{name:V('x_name'),ratePercent:V('x_rate')})),'Tax rule added');
X.xedit=(id,r)=>{const v=prompt('New rate %',r);if(v)go(()=>req('/taxes/'+id,J('PUT',{ratePercent:v})),'Tax updated')};
X.xtog=(id,a)=>go(()=>req('/taxes/'+id,J('PUT',{active:a})),'Tax updated');
// ---- Catalog admin ----
async function rCat(){const [p,pl,d]=await Promise.all([req('/products'),req('/plans'),req('/discounts')]);D.products=p;D.plans=pl;D.discounts=d;
 const tg=(k,x)=>B(x.active?'Deactivate':'Activate',`XA.tog('${k}',${x.id},${!x.active})`);
 $('cat_body').innerHTML='<h3>Products</h3>'+tbl(['Name','Type','Sales','Cost','Status',''],p.map(x=>[H(x.name),H(x.type),M(x.salesPrice),M(x.costPrice),x.active?'Active':'Inactive',B('Edit',`XA.ep(${x.id})`)+tg('products',x)]))
 +'<h3>Plans</h3>'+tbl(['Name','Billing','Price','Min qty','Status',''],pl.map(x=>[H(x.name),x.interval,M(x.price),x.minimumQuantity,x.active?'Active':'Inactive',B('Edit',`XA.epl(${x.id})`)+tg('plans',x)]))
 +'<h3>Discounts</h3>'+tbl(['Name','Value','Valid','Used','Status',''],d.map(x=>[H(x.name),x.kind==='PERCENTAGE'?x.value+'%':M(x.value),`${x.startDate||'…'} → ${x.endDate||'…'}`,`${x.usedCount}/${x.usageLimit??'∞'}`,x.active?'Active':'Inactive',B('Edit',`XA.ed(${x.id})`)+tg('discounts',x)]))}
X.tog=(k,id,a)=>go(()=>req(`/${k}/${id}`,J('PUT',{active:a})),'Updated');
X.ep=id=>{const x=D.products.find(y=>y.id===id),n=prompt('Product name',x.name);if(n===null)return;const pr=prompt('Sales price',x.salesPrice);const c=prompt('Cost price',x.costPrice);go(()=>req('/products/'+id,J('PUT',{name:n,salesPrice:pr,costPrice:c})),'Product updated')};
X.epl=id=>{const x=D.plans.find(y=>y.id===id),n=prompt('Plan name',x.name);if(n===null)return;const pr=prompt('Price',x.price);const q=prompt('Minimum quantity',x.minimumQuantity);go(()=>req('/plans/'+id,J('PUT',{name:n,price:pr,minimumQuantity:q})),'Plan updated')};
X.ed=id=>{const x=D.discounts.find(y=>y.id===id),n=prompt('Discount name',x.name);if(n===null)return;const e=prompt('End date (YYYY-MM-DD, blank for none)',x.endDate||'');const u=prompt('Usage limit (blank for unlimited)',x.usageLimit??'');go(()=>req('/discounts/'+id,J('PUT',{name:n,endDate:e,usageLimit:u})),'Discount updated')};
// ---- Billing & invoices ----
async function rBill(){const s=staff(),inv=await req(s?'/invoices':'/customer/invoices');D.inv=inv;let h='';
 if(s){const f=await req('/billing/failures');h=card(`<b>Recurring billing run</b> (safe to repeat: each period is billed once)<br>${IN('b_date','Bill up to','date',new Date().toISOString().slice(0,10))}${B('Run billing','XA.run()')}`)+(f.length?'<h3>Failed billing attempts</h3>'+tbl(['Subscription','Date','Reason'],f.map(x=>[x.subscriptionId,x.billingDate,H(x.message)])):'')}
 h+='<h3>Invoices</h3>'+tbl(['Invoice','Customer','Period','Due','Total','Paid','Status',''],inv.map(x=>[H(x.number),H(x.customer.name),H(x.periodStart||x.invoiceDate),H(x.dueDate),M(x.total),M(x.paidAmount),badge(x.status),
  (s&&x.status==='DRAFT'?B('Confirm',`XA.ic(${x.id},'confirm')`):'')+(s&&['DRAFT','CONFIRMED'].includes(x.status)?B('Cancel',`XA.ic(${x.id},'cancel')`):'')+(s?B('Send',`XA.is(${x.id})`):'')+B('Print',`XA.ip(${x.id})`)]));$('bill_body').innerHTML=h}
X.run=()=>go(async()=>{const r=await req('/billing/run',J('POST',{asOf:V('b_date')}));toast(`Created ${r.created} invoice(s), ${r.failed} failed`)},'');
X.ic=(id,a)=>go(()=>req(`/invoices/${id}/${a}`,J('POST')),'Invoice updated');
X.is=id=>go(async()=>{const r=await req(`/invoices/${id}/send`,J('POST'));toast(r.message)},'');
X.ip=id=>{const x=D.inv.find(y=>y.id===id),w=window.open('','_blank');if(!w)return toast('Allow pop-ups to print');
 w.document.write(`<title>${H(x.number)}</title><body style="font-family:sans-serif;max-width:600px;margin:30px auto"><h2>Brightloop · Invoice ${H(x.number)}</h2><p>Customer: ${H(x.customer.name)}<br>Date: ${H(x.invoiceDate)} · Due: ${H(x.dueDate)}<br>${H(x.description)}</p><table style="width:100%">
 <tr><td>Subtotal</td><td align=right>${M(x.subtotal)}</td></tr><tr><td>Discount</td><td align=right>-${M(x.discount)}</td></tr><tr><td>Tax</td><td align=right>${M(x.tax)}</td></tr><tr><td><b>Total</b></td><td align=right><b>${M(x.total)}</b></td></tr><tr><td>Paid</td><td align=right>${M(x.paidAmount)}</td></tr></table><p>Status: ${H(x.status)}</p>`);w.document.close();w.print()};
// ---- wiring ----
function init(){const nav=document.querySelector('.nav'),host=$('overview').parentElement;
 const pages=[{id:'quotes',label:'Quotations',ico:'✎',render:rQuotes,roles:['ADMIN','SALES','CUSTOMER']},{id:'bill',label:staff()?'Billing & Invoices':'Invoice printing',ico:'▥',render:rBill,roles:['ADMIN','SALES','CUSTOMER']},
  {id:'tax',label:'Tax Rules',ico:'％',render:rTax,roles:['ADMIN','SALES']},{id:'cat',label:'Manage Catalog',ico:'⚙',render:rCat,roles:['ADMIN']}];
 pages.filter(p=>p.roles.includes(currentUser.role)).forEach(p=>{
  host.insertAdjacentHTML('beforeend',`<section class="section" id="${p.id}"><div class="heading"><div><h1>${p.label}</h1></div></div><div id="${p.id}_body"></div></section>`);
  const b=document.createElement('button');b.dataset.page=p.id;b.innerHTML=`<span class="ico">${p.ico}</span><span class="txt">${p.label}</span>`;
  b.onclick=()=>{navigate(p.id);$('crumbname').textContent=p.label;cur=p;p.render().catch(e=>toast(e.message))};nav.appendChild(b)})}
const t=setInterval(()=>{if(typeof currentUser!=='undefined'&&currentUser){clearInterval(t);init()}},300);
// ---- forgot / reset password on the login screen ----
window.addEventListener('load',()=>{const f=$('loginForm');if(f){const a=document.createElement('a');a.href='#';a.textContent='Forgot password?';a.style.cssText='display:block;text-align:center;margin-top:12px;font-size:13px';
 a.onclick=async e=>{e.preventDefault();const em=prompt('Enter your registered email');if(!em)return;try{await req('/auth/forgot',J('POST',{email:em}));alert('If that email is registered, a reset link was generated. Until an email server is set up it is printed in the server console.')}catch(x){alert(x.message)}};f.appendChild(a)}
 if(location.hash.startsWith('#reset=')){setTimeout(async()=>{const pw=prompt('Enter a new password (more than 8 characters, with upper, lower and a special character)');if(!pw)return;try{await req('/auth/reset',J('POST',{token:location.hash.slice(7),password:pw}));alert('Password updated. Please sign in.');location.hash=''}catch(x){alert(x.message)}},900)}});
})();
