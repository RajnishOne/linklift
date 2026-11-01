#ifndef CONFIGMS_H_INCLUDED
#define CONFIGMS_H_INCLUDED

/* The number of bytes in a double.  */
#define SIZEOF_DOUBLE 8

/* The number of bytes in a float.  */
#define SIZEOF_FLOAT 4

/* The number of bytes in a int.  */
#define SIZEOF_INT 4

/* The number of bytes in a long.  */
#if defined(__LP64__) || defined(_LP64)
#define SIZEOF_LONG 8
#define SIZEOF_UNSIGNED_LONG 8
#define SIZEOF_LONG_DOUBLE 16
#else
#define SIZEOF_LONG 4
#define SIZEOF_UNSIGNED_LONG 4
#define SIZEOF_LONG_DOUBLE 12
#endif

/* The number of bytes in a unsigned short.  */
#define SIZEOF_UNSIGNED_SHORT 2

/* Define if you have the ANSI C header files.  */
#define STDC_HEADERS

/* Define if you have the <errno.h> header file.  */
#define HAVE_ERRNO_H

/* Define if you have the <fcntl.h> header file.  */
#define HAVE_FCNTL_H

/* Define if you have the <limits.h> header file.  */
#define HAVE_LIMITS_H

/* Name of package */
#define PACKAGE "lame"

/* Define if compiler has function prototypes */
#define PROTOTYPES 1

/* faster log implementation with less but enough precission */
#define USE_FAST_LOG 1

#define HAVE_STRCHR
#define HAVE_MEMCPY

#include <stdint.h>
typedef float  float32_t;
typedef double float64_t;

typedef long double ieee854_float80_t;
typedef double      ieee754_float64_t;
typedef float       ieee754_float32_t;

#ifdef HAVE_MPGLIB
# define DECODE_ON_THE_FLY 1
#endif

#ifdef LAME_ACM
/* memory hacking for driver purposes */
#define calloc(x,y) acm_Calloc(x,y)
#define free(x)     acm_Free(x)
#define malloc(x)   acm_Malloc(x)

#include <stddef.h>
void *acm_Calloc( size_t num, size_t size );
void *acm_Malloc( size_t size );
void acm_Free( void * mem);
#endif /* LAME_ACM */

#define LAME_LIBRARY_BUILD


#ifdef HAVE_NASM
    #if (defined(__ICL) && (__ICL >= 450))
        #define HAVE_XMMINTRIN_H
    #elif defined(_MSC_VER)
        #include <malloc.h>
        #ifdef _mm_malloc
            #define HAVE_XMMINTRIN_H
        #endif
    #endif
#endif

#if defined(_M_X64) && !defined(HAVE_XMMINTRIN_H)
        #define HAVE_XMMINTRIN_H
#endif

#endif
