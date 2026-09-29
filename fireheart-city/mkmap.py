import re,sys
L='/mnt/user-data/uploads/meta/libraries/'
pg=open(L+'net/minecraft/client/1.20.1-20230612.114412/client-1.20.1-20230612.114412-mappings.txt').read().splitlines()
ts=open(L+'de/oceanlabs/mcp/mcp_config/1.20.1-20230612.114412/mcp_config-1.20.1-20230612.114412-mappings-merged.txt').read().splitlines()
prim={'void':'V','boolean':'Z','byte':'B','char':'C','short':'S','int':'I','long':'J','float':'F','double':'D'}
cls_n2o={};members={}
cur=None
for l in pg:
    if l.startswith('#') or not l.strip(): continue
    if not l.startswith(' '):
        n,o=l[:-1].split(' -> ');cur=n.replace('.','/');cls_n2o[cur]=o.replace('.','/');members[cur]=[]
    else:
        a,o=l.strip().split(' -> ')
        a=re.sub(r'^\d+:\d+:','',a)
        members[cur].append((a,o))
def td(t,m):
    d=0
    while t.endswith('[]'): t=t[:-2];d+=1
    s=prim.get(t) or 'L'+m(t.replace('.','/'))+';'
    return '['*d+s
ident=lambda x:x
obf=lambda x:cls_n2o.get(x,x)
fmap={};mmap={}
for c,ms in members.items():
    oc=cls_n2o[c]
    for a,o in ms:
        if '(' in a:
            rt,rest=a.split(' ',1);nm,args=rest.split('(',1);args=args[:-1]
            al=[x for x in args.split(',') if x]
            nd='('+''.join(td(x,ident) for x in al)+')'+td(rt,ident)
            od='('+''.join(td(x,obf) for x in al)+')'+td(rt,obf)
            mmap[(oc,o,od)]=(c,nm,nd)
        else:
            t,nm=a.split(' ');fmap[(oc,o)]=(c,nm)
out=[];curo=None;curn=None
for l in ts[1:]:
    if not l.startswith('\t'):
        curo,curn=l.split(' ');continue
    if l.startswith('\t\t'): continue
    p=l.strip().split(' ')
    if len(p)==2:
        o,s=p
        if s.startswith('f_') and (curo,o) in fmap:
            out.append('FD: %s/%s %s/%s'%(curn,s,curn,fmap[(curo,o)][1]))
    elif len(p)==3:
        o,d,s=p
        if s.startswith('m_') and (curo,o,d) in mmap:
            c,nm,nd=mmap[(curo,o,d)]
            out.append('MD: %s/%s %s %s/%s %s'%(curn,s,nd,curn,nm,nd))
open('srg2named.srg','w').write('\n'.join(out)+'\n')
print(len(out))
